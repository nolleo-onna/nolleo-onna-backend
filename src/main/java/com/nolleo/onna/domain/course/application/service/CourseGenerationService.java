package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.port.CourseContentWriter;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.SlotPlan;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import com.nolleo.onna.domain.course.domain.service.DistrictCenter;
import com.nolleo.onna.domain.course.domain.service.SlotPlanner;
import com.nolleo.onna.domain.spot.application.service.SpotSimilarityQueryService;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import com.nolleo.onna.domain.spot.domain.model.vo.SpotCategory;
import com.nolleo.onna.domain.spot.domain.repository.SpotPriceSummaryRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPOT 기반 AI 코스 생성 파이프라인 조율.
 *
 * 순서:
 *   1. 시작 지역 좌표 확정 (DistrictCenter)
 *   2. SlotHints → SlotPlan (카테고리별 목표 개수)
 *   3. 카테고리 그룹별 후보 풀 조회 (거리순, SpotsRepository)
 *   4. mood/companion이 있으면 벡터 유사도로 후보 풀 재정렬, 없으면 거리순 그대로 상위 N개 선택
 *   5. 최근접 탐욕 순서로 코스 조립 (CourseAssembler)
 *   6. FD 카테고리 아이템만 가격 조회 (SpotPriceSummaryRepository)
 *   7. 조립 완료 후 제목·소개 생성 (CourseContentWriter)
 *   8. 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseGenerationService {

    private static final List<SpotCategory> ATTRACTION_CATEGORIES = List.of(
            SpotCategory.NA, SpotCategory.HS, SpotCategory.VE);
    private static final List<SpotCategory> ACTIVITY_CATEGORIES = List.of(
            SpotCategory.EX, SpotCategory.LS);
    private static final int CANDIDATE_POOL_SIZE = 20;

    private final SpotsRepository spotsRepository;
    private final SpotPriceSummaryRepository spotPriceSummaryRepository;
    private final SpotSimilarityQueryService spotSimilarityQueryService;
    private final CourseContentWriter courseContentWriter;
    private final CourseRepository courseRepository;

    public Course generate(Long userId, CourseIntent intent, String createdBy) {
        DistrictCenter center = DistrictCenter.of(intent.startArea())
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 시작 지역: " + intent.startArea()));
        double lat = center.getLatitude();
        double lon = center.getLongitude();

        SlotPlan plan = SlotPlanner.plan(intent.slotHints());
        String queryText = buildRerankQueryText(intent);

        Map<String, Spot> selected = new LinkedHashMap<>();
        selectGroup(List.of(SpotCategory.FD), plan.foodCount() + plan.cafeCount(), lat, lon, queryText, selected);
        selectGroup(ATTRACTION_CATEGORIES, plan.attractionCount(), lat, lon, queryText, selected);
        selectGroup(ACTIVITY_CATEGORIES, plan.activityCount(), lat, lon, queryText, selected);

        List<CourseAssembler.AssembledItem> assembled =
                CourseAssembler.assemble(lat, lon, new ArrayList<>(selected.values()));

        List<String> foodContentIds = assembled.stream()
                .map(CourseAssembler.AssembledItem::spot)
                .filter(spot -> spot.getCategory() == SpotCategory.FD)
                .map(Spot::getContentId)
                .toList();
        Map<String, SpotPriceSummary> priceByContentId = spotPriceSummaryRepository.findAllByIds(foodContentIds);

        Course course = Course.createByAi(userId, UUID.randomUUID(), intent, createdBy);
        for (CourseAssembler.AssembledItem item : assembled) {
            Integer expectedCost = resolveExpectedCost(item.spot(), priceByContentId);
            course.addItem(item.spot().getContentId(), expectedCost, item.distanceFromPrevM());
        }

        List<String> spotTitlesInOrder = assembled.stream()
                .map(item -> item.spot().getTitle())
                .toList();
        CourseContentWriter.CourseContent content = courseContentWriter.generate(intent, spotTitlesInOrder);
        course.applyAiContent(content.title(), content.description());

        return courseRepository.save(course);
    }

    private Integer resolveExpectedCost(Spot spot, Map<String, SpotPriceSummary> priceByContentId) {
        if (spot.getCategory() != SpotCategory.FD) return null;
        SpotPriceSummary price = priceByContentId.get(spot.getContentId());
        if (price == null) return null;
        if (price.getMinPrice() != null) return price.getMinPrice();
        return price.getRepresentativePrice();
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
    private void selectGroup(List<SpotCategory> categories, int count, double lat, double lon,
                              String queryText, Map<String, Spot> selected) {
        if (count <= 0) return;

        List<Spot> pool = new ArrayList<>();
        for (SpotCategory category : categories) {
            pool.addAll(spotsRepository.findNearbyByCategory(category.name(), lat, lon));
        }
        pool = pool.stream()
                .filter(spot -> !selected.containsKey(spot.getContentId()))
                .limit(CANDIDATE_POOL_SIZE)
                .toList();
        if (pool.isEmpty()) return;

        List<Spot> chosen;
        if (queryText != null) {
            List<String> poolIds = pool.stream().map(Spot::getContentId).toList();
            List<SpotSimilarity> ranked = spotSimilarityQueryService.rerankByQuery(queryText, poolIds);
            Map<String, Spot> poolByContentId = new LinkedHashMap<>();
            pool.forEach(spot -> poolByContentId.put(spot.getContentId(), spot));
            chosen = ranked.stream()
                    .map(similarity -> poolByContentId.get(similarity.contentId()))
                    .filter(java.util.Objects::nonNull)
                    .limit(count)
                    .toList();
        } else {
            chosen = pool.stream().limit(count).toList();
        }
        chosen.forEach(spot -> selected.put(spot.getContentId(), spot));
    }
}
