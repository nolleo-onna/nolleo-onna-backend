package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.CourseContentWriter;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.SlotPlan;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import com.nolleo.onna.domain.course.domain.service.SlotPlanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
 *   3. 카테고리 그룹별 후보 풀 조회 (SpotLookupPort) — 검색 반경(nearbyAllowed) 안에서 가까운 순
 *      상위 CANDIDATE_POOL_SIZE개. 정렬·절단은 DB(PostGIS KNN)가 끝내므로 여기서 다시 정렬하지 않는다.
 *   4. mood/companion이 있으면 벡터 유사도로 후보 풀 재정렬, 없으면 거리순 그대로 상위 N개 선택.
 *      쿼리 텍스트는 그룹마다 같으므로 임베딩(SpotReranker.prepare)은 요청당 1회만 한다.
 *   5. 최근접 탐욕 순서로 코스 조립 (CourseAssembler)
 *   6. FD 카테고리 아이템만 가격 조회 (SpotLookupPort)
 *   7. 조립 완료 후 제목·소개 생성 (CourseContentWriter)
 *   8. 저장
 *
 * intent.budget은 스냅샷으로 저장만 하고 후보 선택·가격 필터에는 아직 반영하지 않는다 (#71에서 미반영으로 결정).
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
    /** 그룹당 후보 풀 크기 — DB가 가까운 순으로 이만큼만 잘라서 준다 */
    private static final int CANDIDATE_POOL_SIZE = 20;

    /** 후보 검색 반경(미터) — 시작 지역 중심 기준. 지역명만 말하면 그 지역 안에서 고른다 */
    static final double SEARCH_RADIUS_M = 5_000;

    /** "근처도 괜찮아"(nearbyAllowed)일 때의 검색 반경(미터) — 인접 지역까지 넓힌다 */
    static final double NEARBY_SEARCH_RADIUS_M = 15_000;

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
        double radiusM = intent.nearbyAllowed() ? NEARBY_SEARCH_RADIUS_M : SEARCH_RADIUS_M;

        // 무드·동행 쿼리는 그룹마다 같으므로 임베딩은 여기서 한 번만 한다 — 그룹마다 하면 같은 텍스트로 API를 3번 부른다
        String queryText = buildRerankQueryText(intent);
        SpotReranker.Ranker ranker = queryText != null ? spotReranker.prepare(queryText) : null;

        Map<String, SpotCandidate> selected = new LinkedHashMap<>();
        selectGroup(List.of(FOOD_CATEGORY), plan.foodCount() + plan.cafeCount(), lat, lon, radiusM, ranker, selected);
        selectGroup(ATTRACTION_CATEGORIES, plan.attractionCount(), lat, lon, radiusM, ranker, selected);
        selectGroup(ACTIVITY_CATEGORIES, plan.activityCount(), lat, lon, radiusM, ranker, selected);

        // SpotCandidate → Course 컨텍스트의 Waypoint VO 변환 (좌표 없는 스팟은 제외)
        List<CourseAssembler.Waypoint> waypoints = selected.values().stream()
                .map(CourseGenerationService::toWaypoint)
                .filter(Objects::nonNull)
                .toList();

        List<CourseAssembler.AssembledItem> assembled = CourseAssembler.assemble(lat, lon, waypoints);
        if (assembled.isEmpty()) {
            // 반경 안에 활성 스팟이 하나도 없다 — 빈 코스를 저장하지 않고 사용자에게 조건 변경을 안내한다
            throw new BusinessException(CourseErrorCode.NO_SPOT_CANDIDATES);
        }
        // 편집 경로와 같은 불변식(최대 개수·중복 금지) — 규칙은 CoursePlaces 한 곳에만 있다
        new CoursePlaces(assembled.stream().map(item -> PlaceRef.spot(item.waypoint().refId())).toList());

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
            course.addItem(PlaceRef.spot(spot.contentId()), expectedCost, item.distanceFromPrevM());
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

    /**
     * 카테고리 그룹의 후보 풀을 조회해 count개를 선택해 selected에 누적한다.
     * 그룹의 카테고리를 한 쿼리로 묶어 조회하므로 풀은 그룹 전체 기준 거리순이고, DB가 이미
     * CANDIDATE_POOL_SIZE개로 잘라서 주기 때문에 여기서는 정렬도 절단도 하지 않는다.
     */
    private void selectGroup(List<String> categories, int count, double lat, double lon, double radiusM,
                             SpotReranker.Ranker ranker, Map<String, SpotCandidate> selected) {
        if (count <= 0) return;

        // 한 스팟은 카테고리가 하나라 그룹 간 중복은 원칙적으로 없지만, 데이터 이상에 대비해 걸러 둔다
        List<SpotCandidate> pool = spotLookupPort
                .findNearbyByCategories(categories, lat, lon, radiusM, CANDIDATE_POOL_SIZE).stream()
                .filter(spot -> !selected.containsKey(spot.contentId()))
                .filter(SpotCandidate::hasCoordinate)
                .toList();
        if (pool.isEmpty()) return;

        List<SpotCandidate> ordered = ranker != null ? rerank(pool, ranker) : pool;
        ordered.stream().limit(count).forEach(spot -> selected.put(spot.contentId(), spot));
    }

    /**
     * 후보 풀을 리랭킹 순서로 재배열한다.
     * 리랭킹 결과에 없는 후보(임베딩 미적재 등)는 버리지 않고 거리순 그대로 뒤에 이어 붙인다 —
     * 조용히 탈락시키면 요청한 개수보다 적은 코스가 나온다.
     */
    private static List<SpotCandidate> rerank(List<SpotCandidate> pool, SpotReranker.Ranker ranker) {
        Map<String, SpotCandidate> remaining = new LinkedHashMap<>();
        pool.forEach(spot -> remaining.put(spot.contentId(), spot));

        List<SpotCandidate> ordered = new ArrayList<>(pool.size());
        for (String contentId : ranker.rerank(List.copyOf(remaining.keySet()))) {
            SpotCandidate spot = remaining.remove(contentId);
            if (spot != null) ordered.add(spot);
        }
        ordered.addAll(remaining.values());
        return ordered;
    }
}
