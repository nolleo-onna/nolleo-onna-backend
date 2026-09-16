package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.GenerationOptions;
import com.nolleo.onna.domain.course.application.dto.GenerationResult;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.CourseContentWriter;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.BudgetTier;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.SlotPlan;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
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
import java.util.Set;
import java.util.UUID;

/**
 * SPOT 기반 코스 생성 파이프라인 조율 — 챗봇(AI)과 폼(ALGORITHM)이 공유한다. 경로별 차이는 GenerationOptions로 받는다.
 *
 * 순서:
 *   1. 검색 중심 좌표 확정 — 기준점("X 근처")을 찾았으면 그 좌표, 아니면 시작 지역 중심 (CourseIntent.center)
 *   2. 예산 등급 + SlotHints → SlotPlan (카테고리별 목표 개수). 예산 등급이 기본 슬롯을, 사용자 힌트가 덮어쓴다
 *   2-1. 사용자가 이름으로 지정한 스팟 — 확인 단계에서 못 끝낸 매칭을 마저 하고(SpotPinResolver), contentId로 일괄 조회한다.
 *        포함 스팟은 미리 선택에 담아 해당 카테고리 슬롯을 차지하고(예산과 무관), 제외 스팟은 후보 풀에서 걸러낸다. 못 찾은 지정은 건너뛴다
 *   3. 카테고리 그룹별 후보 풀 조회 (SpotLookupPort) — 검색 반경(nearbyAllowed) 안에서 가까운 순
 *      상위 CANDIDATE_POOL_SIZE개. 정렬·절단은 DB(PostGIS KNN)가 끝내므로 여기서 다시 정렬하지 않는다.
 *   3-1. 식사·카페(FD) 풀에는 예산 등급의 1곳당 상한으로 가격 필터를 건다 — 가격 정보 없는 스팟은 통과,
 *        필터 후 후보가 슬롯보다 적으면 필터를 풀고 채운다 (GenerationResult.budgetFilterRelaxed)
 *   4. 리랭킹을 쓰는 경로에서 mood/companion이 있으면 벡터 유사도로 후보 풀 재정렬, 아니면 거리순 그대로 상위 N개 선택.
 *      쿼리 텍스트는 그룹마다 같으므로 임베딩(SpotReranker.prepare)은 요청당 1회만 한다.
 *   5. 최근접 탐욕 순서로 코스 조립 (CourseAssembler)
 *   6. FD 카테고리 아이템만 가격 조회 (SpotLookupPort)
 *   7. 제목·소개 — 챗봇은 조립 후 AI(CourseContentWriter), 폼은 템플릿 제목 + 소개 없음
 *   8. 저장
 *
 * Spot 컨텍스트에는 SpotLookupPort/SpotReranker 포트로만 접근한다 —
 * Spot의 도메인 모델(Spot/SpotCategory/GeoCoordinate)을 이 클래스가 직접 알지 않는다.
 *
 * 트랜잭션 정책:
 *   이 클래스에는 트랜잭션을 걸지 않는다. 4·7단계에서 외부 AI API를 호출할 수 있으므로
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
    private static final List<String> FOOD_CATEGORIES = List.of("FD");
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
    private final SpotPinResolver spotPinResolver;

    /** 챗봇 경로 — 기존 호출부 호환 */
    public Course generate(Long userId, CourseIntent rawIntent, String createdBy) {
        return generate(userId, rawIntent, GenerationOptions.aiChat()).course();
    }

    public GenerationResult generate(Long userId, CourseIntent rawIntent, GenerationOptions options) {
        // 검색 중심 — 기준점("X 근처")을 찾았으면 그 좌표, 아니면 시작 지역 중심
        GeoPoint center = rawIntent.center()
                .orElseThrow(() -> new BusinessException(CourseErrorCode.UNKNOWN_START_AREA));
        double lat = center.latitude();
        double lon = center.longitude();

        // 지정 장소 매칭 — 확인 단계에서 이미 끝났으면 그대로, 남은 미해결 지정만 한 번 더 시도한다 (확인을 거치지 않는 경로 대비).
        // 매칭 결과가 들어간 intent를 스냅샷으로 저장해 어떤 스팟으로 이해했는지 재현할 수 있게 한다
        CourseIntent intent = spotPinResolver.resolve(rawIntent);

        BudgetTier budget = BudgetTier.fromAmount(intent.budget());
        SlotPlan plan = SlotPlanner.plan(intent.slotHints(), budget);
        double radiusM = intent.nearbyAllowed() ? NEARBY_SEARCH_RADIUS_M : SEARCH_RADIUS_M;

        // 무드·동행 쿼리는 그룹마다 같으므로 임베딩은 여기서 한 번만 한다 — 그룹마다 하면 같은 텍스트로 API를 3번 부른다
        String queryText = options.useRerank() ? buildRerankQueryText(intent) : null;
        SpotReranker.Ranker ranker = queryText != null ? spotReranker.prepare(queryText) : null;

        // 사용자가 이름으로 지정한 스팟 — 포함은 후보 선택 전에 미리 담고, 제외는 후보 풀에서 걸러낸다.
        // 매칭된 pin은 contentId로 일괄 조회한다. 끝내 못 찾은 지정은 로그만 남기고 건너뛴다(코스 생성은 계속) —
        // 사용자에게는 확인 단계(챗봇) 또는 응답(폼)에서 알린다.
        Map<String, SpotCandidate> pinned = lookupPinned(intent.includeSpots(), "포함");
        Set<String> excludedIds = lookupPinned(intent.excludeSpots(), "제외").keySet();

        Map<String, SpotCandidate> selected = new LinkedHashMap<>();
        pinned.values().stream()
                .filter(spot -> !excludedIds.contains(spot.contentId()))
                .forEach(spot -> selected.put(spot.contentId(), spot));

        // 지정 스팟은 해당 카테고리 그룹의 슬롯을 차지한다 — "관광지 1곳"에 광안리해수욕장을 넣었으면 관광지는 더 고르지 않는다
        boolean budgetFilterRelaxed = selectFoodGroup(plan.foodCount() + plan.cafeCount() - countIn(selected, FOOD_CATEGORIES),
                budget, lat, lon, radiusM, ranker, excludedIds, selected);
        selectGroup(ATTRACTION_CATEGORIES, plan.attractionCount() - countIn(selected, ATTRACTION_CATEGORIES),
                lat, lon, radiusM, ranker, excludedIds, selected);
        selectGroup(ACTIVITY_CATEGORIES, plan.activityCount() - countIn(selected, ACTIVITY_CATEGORIES),
                lat, lon, radiusM, ranker, excludedIds, selected);

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

        Course course = options.usesAiContent()
                ? Course.createByAi(userId, UUID.randomUUID(), intent, options.createdBy())
                : Course.createByForm(userId, UUID.randomUUID(), options.templateTitle(), intent, options.createdBy());
        for (CourseAssembler.AssembledItem item : assembled) {
            SpotCandidate spot = selected.get(item.waypoint().refId());
            Integer expectedCost = spot.isFood() ? priceByContentId.get(spot.contentId()) : null;
            course.addItem(PlaceRef.spot(spot.contentId()), expectedCost, item.distanceFromPrevM());
        }

        if (options.usesAiContent()) {
            List<String> spotTitlesInOrder = assembled.stream()
                    .map(item -> selected.get(item.waypoint().refId()).title())
                    .toList();
            CourseContentWriter.CourseContent content = courseContentWriter.generate(intent, spotTitlesInOrder);
            course.applyAiContent(content.title(), content.description());
        }

        return new GenerationResult(courseRepository.save(course), budgetFilterRelaxed);
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

    /**
     * 매칭된 지정 장소(SpotPin)의 스팟을 contentId로 일괄 조회한다 (contentId → 후보). 같은 스팟으로 풀리는 지정은 한 번만 담는다.
     * 미해결 pin(끝내 못 찾은 이름)과 그 사이 비활성화된 스팟은 건너뛴다 — 지정 하나 때문에 코스 생성 전체를 실패시키지 않는다.
     */
    private Map<String, SpotCandidate> lookupPinned(List<SpotPin> pins, String purpose) {
        List<String> knownIds = pins.stream().filter(SpotPin::isResolved).map(SpotPin::contentId).distinct().toList();
        Map<String, SpotCandidate> activeById = knownIds.isEmpty() ? Map.of() : spotLookupPort.findActiveByIds(knownIds);

        Map<String, SpotCandidate> found = new LinkedHashMap<>();
        for (SpotPin pin : pins) {
            if (!pin.isResolved()) {
                log.warn("{} 지정 스팟을 찾지 못해 건너뜀 name={}", purpose, pin.name());
                continue;
            }
            SpotCandidate spot = activeById.get(pin.contentId());
            if (spot == null) {
                log.warn("{} 지정 스팟이 더 이상 활성이 아니라 건너뜀 name={} contentId={}", purpose, pin.name(), pin.contentId());
                continue;
            }
            found.put(spot.contentId(), spot);
        }
        return found;
    }

    /** selected 중 카테고리가 categories에 속하는 스팟 수 — 지정 스팟이 차지한 슬롯 계산용 */
    private static int countIn(Map<String, SpotCandidate> selected, List<String> categories) {
        return (int) selected.values().stream()
                .filter(spot -> spot.categoryCode() != null && categories.contains(spot.categoryCode()))
                .count();
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
     * 식사·카페(FD) 그룹 — 예산 등급에 1곳당 상한이 있으면 가격 필터를 건 뒤 고른다.
     * 가격 정보가 없는 스팟은 통과시키고, 필터 후 후보가 count보다 적으면 필터를 풀고 채운다.
     *
     * @return 필터를 풀었는지 (응답에서 "예산 안에서 다 채우지 못했다"고 알리기 위해)
     */
    private boolean selectFoodGroup(int count, BudgetTier budget, double lat, double lon, double radiusM,
                                    SpotReranker.Ranker ranker, Set<String> excludedIds,
                                    Map<String, SpotCandidate> selected) {
        if (count <= 0) return false;

        List<SpotCandidate> pool = fetchPool(FOOD_CATEGORIES, lat, lon, radiusM, excludedIds, selected);
        if (pool.isEmpty()) return false;

        boolean relaxed = false;
        if (budget.hasPriceCap()) {
            Map<String, Integer> priceById = spotLookupPort.findFoodPrices(pool.stream().map(SpotCandidate::contentId).toList());
            List<SpotCandidate> affordable = pool.stream()
                    .filter(spot -> {
                        Integer price = priceById.get(spot.contentId());
                        return price == null || price <= budget.perItemCap();
                    })
                    .toList();
            if (affordable.size() >= count) {
                pool = affordable;
            } else {
                relaxed = true;
                log.info("예산 필터 해제 — 상한 {}원 이하 후보 {}개 < 필요 {}개", budget.perItemCap(), affordable.size(), count);
            }
        }

        choose(pool, count, ranker, selected);
        return relaxed;
    }

    /**
     * 카테고리 그룹의 후보 풀을 조회해 count개를 선택해 selected에 누적한다.
     * 그룹의 카테고리를 한 쿼리로 묶어 조회하므로 풀은 그룹 전체 기준 거리순이고, DB가 이미
     * CANDIDATE_POOL_SIZE개로 잘라서 주기 때문에 여기서는 정렬도 절단도 하지 않는다.
     */
    private void selectGroup(List<String> categories, int count, double lat, double lon, double radiusM,
                             SpotReranker.Ranker ranker, Set<String> excludedIds,
                             Map<String, SpotCandidate> selected) {
        if (count <= 0) return;
        List<SpotCandidate> pool = fetchPool(categories, lat, lon, radiusM, excludedIds, selected);
        if (pool.isEmpty()) return;
        choose(pool, count, ranker, selected);
    }

    /**
     * 한 스팟은 카테고리가 하나라 그룹 간 중복은 원칙적으로 없지만, 데이터 이상에 대비해 걸러 둔다.
     * 사용자가 빼달라고 한 스팟은 후보 풀에서 제거한다 (풀은 DB가 20개로 잘라 주므로 제외만큼 후보가 줄 수 있다)
     */
    private List<SpotCandidate> fetchPool(List<String> categories, double lat, double lon, double radiusM,
                                          Set<String> excludedIds, Map<String, SpotCandidate> selected) {
        return spotLookupPort
                .findNearbyByCategories(categories, lat, lon, radiusM, CANDIDATE_POOL_SIZE).stream()
                .filter(spot -> !selected.containsKey(spot.contentId()))
                .filter(spot -> !excludedIds.contains(spot.contentId()))
                .filter(SpotCandidate::hasCoordinate)
                .toList();
    }

    private static void choose(List<SpotCandidate> pool, int count, SpotReranker.Ranker ranker,
                               Map<String, SpotCandidate> selected) {
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
