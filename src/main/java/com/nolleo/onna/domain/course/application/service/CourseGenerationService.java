package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.CourseContentWriter;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.SlotPlan;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import com.nolleo.onna.domain.course.domain.service.SlotPlanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * SPOT 기반 AI 코스 생성 파이프라인 조율.
 *
 * 순서:
 *   1. 시작 지역 좌표 확정 (DistrictCenter)
 *   2. SlotHints → SlotPlan (카테고리별 목표 개수)
 *   3. 카테고리 그룹별 후보 풀 조회 (거리순, SpotLookupPort)
 *   4. mood/companion이 있으면 벡터 유사도로 후보 풀 재정렬, 없으면 거리순 그대로 상위 N개 선택
 *   5. 최근접 탐욕 순서로 코스 조립 (CourseAssembler)
 *   6. FD 카테고리 아이템만 가격 조회 (SpotLookupPort)
 *   7. 조립 완료 후 제목·소개 생성 (CourseContentWriter)
 *   8. 저장
 *
 * Spot 컨텍스트에는 SpotLookupPort/SpotReranker 포트로만 접근한다 —
 * Spot의 도메인 모델(Spot/SpotCategory/GeoCoordinate)을 이 클래스가 직접 알지 않는다.
 *
 * 트랜잭션 정책:
 *   이 클래스에는 트랜잭션을 걸지 않는다. 4·7단계에서 외부 AI API를 호출하므로
 *   전체를 트랜잭션으로 묶으면 응답을 기다리는 수 초 동안 DB 커넥션을 점유해
 *   동시 요청 몇 건만으로도 커넥션 풀이 고갈된다.
 *   3·6단계 조회는 각각 독립적이라 하나의 트랜잭션이 필요하지 않고,
 *   마지막 저장은 CourseEntity의 cascade=ALL 덕분에 save() 한 번으로 끝나므로
 *   Spring Data의 SimpleJpaRepository.save()가 여는 트랜잭션만으로 원자성이 보장된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseGenerationService {

    private static final List<String> ATTRACTION_CATEGORIES = List.of("NA", "HS", "VE");
    private static final List<String> ACTIVITY_CATEGORIES = List.of("EX", "LS");
    private static final String FOOD_CATEGORY = "FD";
    private static final int CANDIDATE_POOL_SIZE = 20;

    private final SpotLookupPort spotLookupPort;
    private final SpotReranker spotReranker;
    private final CourseContentWriter courseContentWriter;
    private final CourseRepository courseRepository;

    public Course generate(Long userId, CourseIntent intent, String createdBy) {
        DistrictCenter center = DistrictCenter.of(intent.startArea())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.UNKNOWN_START_AREA));
        double lat = center.getLatitude();
        double lon = center.getLongitude();

        SlotPlan plan = SlotPlanner.plan(intent.slotHints());
        String queryText = buildRerankQueryText(intent);

        Map<String, SpotCandidate> selected = new LinkedHashMap<>();
        selectGroup(List.of(FOOD_CATEGORY), plan.foodCount() + plan.cafeCount(), lat, lon, queryText, selected);
        selectGroup(ATTRACTION_CATEGORIES, plan.attractionCount(), lat, lon, queryText, selected);
        selectGroup(ACTIVITY_CATEGORIES, plan.activityCount(), lat, lon, queryText, selected);

        // SpotCandidate → Course 컨텍스트의 Waypoint VO 변환 (좌표 없는 스팟은 제외)
        List<CourseAssembler.Waypoint> waypoints = selected.values().stream()
                .map(CourseGenerationService::toWaypoint)
                .filter(Objects::nonNull)
                .toList();

        List<CourseAssembler.AssembledItem> assembled = CourseAssembler.assemble(lat, lon, waypoints);

        List<String> foodContentIds = assembled.stream()
                .map(item -> selected.get(item.waypoint().refId()))
                .filter(SpotCandidate::isFood)
                .map(SpotCandidate::contentId)
                .toList();
        Map<String, Integer> priceByContentId = spotLookupPort.findFoodPrices(foodContentIds);

        Course course = Course.createByAi(userId, UUID.randomUUID(), intent, createdBy);
        for (CourseAssembler.AssembledItem item : assembled) {
            SpotCandidate spot = selected.get(item.waypoint().refId());
            Integer expectedCost = spot.isFood() ? priceByContentId.get(spot.contentId()) : null;
            course.addItem(spot.contentId(), expectedCost, item.distanceFromPrevM());
        }

        List<String> spotTitlesInOrder = assembled.stream()
                .map(item -> selected.get(item.waypoint().refId()).title())
                .toList();
        CourseContentWriter.CourseContent content = courseContentWriter.generate(intent, spotTitlesInOrder);
        course.applyAiContent(content.title(), content.description());

        return courseRepository.save(course);
    }

    /** 코스 조립에 필요한 좌표만 추출한다. 좌표가 없으면 null. */
    private static CourseAssembler.Waypoint toWaypoint(SpotCandidate spot) {
        if (!spot.hasCoordinate()) {
            log.warn("좌표 없는 스팟 제외 contentId={}", spot.contentId());
            return null;
        }
        return new CourseAssembler.Waypoint(
                spot.contentId(),
                spot.mapY().doubleValue(),
                spot.mapX().doubleValue());
    }

    /** 시작 좌표로부터 스팟까지의 직선 거리(미터). 후보 풀 정렬용. */
    private static double distanceFrom(double lat, double lon, SpotCandidate spot) {
        return CourseAssembler.distanceMeters(lat, lon, spot.mapY().doubleValue(), spot.mapX().doubleValue());
    }

    private String buildRerankQueryText(CourseIntent intent) {
        if (intent.mood().isEmpty() && intent.companion() == null) return null;
        StringBuilder sb = new StringBuilder();
        if (!intent.mood().isEmpty()) sb.append(String.join(", ", intent.mood()));
        if (intent.companion() != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(intent.companion()).append(" 여행");
        }
        return sb.toString();
    }

    /** 카테고리 그룹의 후보 풀을 조회해 count개를 선택해 selected에 누적한다. */
    private void selectGroup(List<String> categories, int count, double lat, double lon,
                              String queryText, Map<String, SpotCandidate> selected) {
        if (count <= 0) return;

        List<SpotCandidate> pool = new ArrayList<>();
        for (String category : categories) {
            pool.addAll(spotLookupPort.findNearbyByCategory(category, lat, lon));
        }
        // 카테고리별 조회 결과를 순서대로 이어 붙였기 때문에 병합된 목록은 거리순이 아니다.
        // 정렬 없이 앞에서 자르면 먼저 조회한 카테고리가 후보 풀을 독점해
        // 뒤쪽 카테고리(예: HS/VE)가 한 건도 후보에 들어가지 못한다.
        pool = pool.stream()
                .filter(spot -> !selected.containsKey(spot.contentId()))
                .filter(SpotCandidate::hasCoordinate)
                .sorted(Comparator.comparingDouble(spot -> distanceFrom(lat, lon, spot)))
                .limit(CANDIDATE_POOL_SIZE)
                .toList();
        if (pool.isEmpty()) return;

        List<SpotCandidate> chosen;
        if (queryText != null) {
            List<String> poolIds = pool.stream().map(SpotCandidate::contentId).toList();
            List<String> rankedIds = spotReranker.rerank(queryText, poolIds);
            Map<String, SpotCandidate> poolByContentId = new LinkedHashMap<>();
            pool.forEach(spot -> poolByContentId.put(spot.contentId(), spot));
            chosen = rankedIds.stream()
                    .map(poolByContentId::get)
                    .filter(Objects::nonNull)
                    .limit(count)
                    .toList();
        } else {
            chosen = pool.stream().limit(count).toList();
        }
        chosen.forEach(spot -> selected.put(spot.contentId(), spot));
    }
}
