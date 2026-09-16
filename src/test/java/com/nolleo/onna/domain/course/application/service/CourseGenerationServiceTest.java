package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.GenerationOptions;
import com.nolleo.onna.domain.course.application.dto.GenerationResult;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.CourseContentWriter;
import com.nolleo.onna.domain.course.application.port.CourseContentWriter.CourseContent;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.SlotHints;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.service.DiverseSpotSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourseGenerationServiceTest {

    @Mock SpotLookupPort spotLookupPort;
    @Mock SpotReranker spotReranker;
    @Mock SpotReranker.Ranker ranker;
    @Mock CourseContentWriter courseContentWriter;
    @Mock CourseRepository courseRepository;

    private CourseGenerationService service;

    /**
     * 난수가 항상 0인 생성기 — 선택기가 가중치와 무관하게 남은 후보 중 최상위를 뽑는다.
     * 기존 테스트가 "순위 1위가 뽑힌다"를 전제로 쓰므로 기본 서비스는 이것으로 만든다. 무작위성 자체는 별도 테스트에서 시드로 본다.
     */
    private static final RandomGenerator ALWAYS_FIRST = () -> 0L;

    @BeforeEach
    void setUp() {
        service = serviceWith(ALWAYS_FIRST);
    }

    /** SpotPinResolver는 실제 객체 — 같은 spotLookupPort 목을 쓰므로 이름 검색 스텁은 한 곳에서 관리된다 */
    private CourseGenerationService serviceWith(RandomGenerator random) {
        return new CourseGenerationService(spotLookupPort, spotReranker, courseContentWriter, courseRepository,
                new SpotPinResolver(spotLookupPort), new DiverseSpotSelector(random));
    }

    // 픽스처 — 광안리 중심(35.1531, 129.1187) 기준. SpotCandidate는 mapX=경도, mapY=위도.

    private static final double GWANGAN_LAT = 35.1531;
    private static final double GWANGAN_LON = 129.1187;
    private static final int POOL_SIZE = CourseGenerationService.CANDIDATE_POOL_SIZE;

    /** 서비스가 그룹별로 묶어 조회하는 카테고리 목록 — 포트 호출 인자와 정확히 일치해야 한다 */
    private static final List<String> FOOD_GROUP = List.of("FD");
    private static final List<String> ATTRACTION_GROUP = List.of("NA", "HS", "VE");

    private static SpotCandidate spot(String id, String category, double lat, double lon) {
        return new SpotCandidate(id, id + "명", null, category, category,
                BigDecimal.valueOf(lon), BigDecimal.valueOf(lat));
    }

    private static final SpotCandidate FOOD_NEAR = spot("food1", "FD", 35.1540, 129.1190);   // 약 100m
    private static final SpotCandidate FOOD_FAR  = spot("food2", "FD", 35.1700, 129.1400);   // 약 2.8km
    private static final SpotCandidate NATURE    = spot("nature1", "NA", 35.1560, 129.1200); // 약 300m
    private static final SpotCandidate NO_COORD  = new SpotCandidate("ghost", "유령", null, "FD", "FD", null, null);

    /** 슬롯을 명시해 그룹별 호출을 통제한다. attraction/activity가 0이면 해당 그룹은 조회조차 하지 않는다. */
    private static CourseIntent intent(int food, int attraction, List<String> mood, String companion) {
        return intent(food, attraction, mood, companion, false);
    }

    private static CourseIntent intent(int food, int attraction, List<String> mood, String companion,
                                       boolean nearbyAllowed) {
        return new CourseIntent("광안리", nearbyAllowed, null, companion, mood,
                new SlotHints(food, 0, attraction, 0), false);
    }

    /** 사용자가 이름으로 지정한 포함·제외 장소가 있는 intent — 아직 매칭하지 않은(미해결) 지정 */
    private static CourseIntent intentWithPins(int food, int attraction, List<String> include, List<String> exclude) {
        return intentWithResolvedPins(food, attraction,
                include.stream().map(SpotPin::of).toList(), exclude.stream().map(SpotPin::of).toList());
    }

    private static CourseIntent intentWithResolvedPins(int food, int attraction, List<SpotPin> include, List<SpotPin> exclude) {
        return new CourseIntent("광안리", false, null, null, List.of(),
                new SlotHints(food, 0, attraction, 0), false, include, exclude);
    }

    private static final SpotCandidate BEACH = spot("beach", "NA", 35.1532, 129.1188);      // 광안리해수욕장, 관광지
    private static final SpotCandidate HAEUNDAE = spot("haeundae", "NA", 35.1587, 129.1604); // 해운대해수욕장, 관광지

    private void stubSaveReturnsArgument() {
        given(courseRepository.save(any(Course.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private void stubContent() {
        given(courseContentWriter.generate(any(CourseIntent.class), anyList()))
                .willReturn(new CourseContent("생성된 제목", "생성된 소개"));
    }

    private void stubFoodPool(List<SpotCandidate> pool) {
        given(spotLookupPort.findNearbyByCategories(eq(FOOD_GROUP), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(pool);
    }

    private void stubAttractionPool(List<SpotCandidate> pool) {
        given(spotLookupPort.findNearbyByCategories(eq(ATTRACTION_GROUP), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(pool);
    }

    @Test
    @DisplayName("스팟을 최근접 순으로 조립해 SPOT 참조 아이템으로 저장하고, FD만 가격을 붙여 총비용을 계산한다")
    void generate_assemblesAndSaves() {
        // given
        stubFoodPool(List.of(FOOD_NEAR));
        stubAttractionPool(List.of(NATURE));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of("food1", 12000));
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course result = service.generate(1L, intent(1, 1, List.of(), null), "AI_CHAT");

        // then
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertThat(result).isSameAs(saved);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getGenerationMode()).isEqualTo(GenerationMode.AI);
        assertThat(saved.getPairId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("생성된 제목");
        assertThat(saved.getDescription()).isEqualTo("생성된 소개");

        List<CourseItem> items = saved.getItems();
        // 최근접 탐욕: food1(100m) → nature1(300m)
        assertThat(items).extracting(CourseItem::getPlaceRef)
                .containsExactly(PlaceRef.spot("food1"), PlaceRef.spot("nature1"));
        assertThat(items).extracting(CourseItem::getSerialNum).containsExactly((short) 1, (short) 2);
        assertThat(items.get(0).getExpectedCost()).isEqualTo(12000);
        assertThat(items.get(1).getExpectedCost()).isNull();
        assertThat(saved.getTotalCost()).isEqualTo(12000);
        assertThat(items).allSatisfy(item -> assertThat(item.getDistanceFromPrevM()).isNotNegative());

        // 제목 생성에는 방문 순서대로의 스팟명이 전달된다
        verify(courseContentWriter).generate(any(CourseIntent.class), eq(List.of("food1명", "nature1명")));
        // mood·companion이 없으면 임베딩(리랭킹 준비)을 하지 않는다
        verify(spotReranker, never()).prepare(anyString());
    }

    @Test
    @DisplayName("지원하지 않는 지역이면 UNKNOWN_START_AREA를 던지고 아무것도 저장하지 않는다")
    void generate_throws_whenStartAreaUnknown() {
        CourseIntent unknown = new CourseIntent("화성", false, null, null, List.of(), null, false);

        assertThatThrownBy(() -> service.generate(1L, unknown, "AI_CHAT"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.UNKNOWN_START_AREA);

        verifyNoInteractions(spotLookupPort, spotReranker, courseRepository, courseContentWriter);
    }

    @Test
    @DisplayName("후보 조회는 시작 지역 중심 좌표·기본 반경·풀 크기를 그대로 포트에 넘긴다 (정렬·절단은 DB 책임)")
    void generate_queriesWithCenterDefaultRadiusAndPoolSize() {
        // given
        stubFoodPool(List.of(FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        service.generate(1L, intent(1, 0, List.of(), null), "AI_CHAT");

        // then
        verify(spotLookupPort).findNearbyByCategories(
                FOOD_GROUP, GWANGAN_LAT, GWANGAN_LON, CourseGenerationService.SEARCH_RADIUS_M, POOL_SIZE);
    }

    @Test
    @DisplayName("nearbyAllowed(근처도 괜찮아)면 넓은 반경으로 후보를 조회한다")
    void generate_widensRadius_whenNearbyAllowed() {
        // given
        stubFoodPool(List.of(FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        service.generate(1L, intent(1, 0, List.of(), null, true), "AI_CHAT");

        // then
        verify(spotLookupPort).findNearbyByCategories(
                FOOD_GROUP, GWANGAN_LAT, GWANGAN_LON, CourseGenerationService.NEARBY_SEARCH_RADIUS_M, POOL_SIZE);
    }

    @Test
    @DisplayName("리랭킹이 없으면 후보 풀의 순서(DB 거리순)를 그대로 신뢰해 앞에서부터 고른다 — 서비스가 다시 정렬하지 않는다")
    void generate_keepsPoolOrder_whenNoRerank() {
        // given — 포트가 준 순서는 [food2, food1]. 좌표상으로는 food1이 더 가깝지만 서비스는 순서를 바꾸지 않는다
        stubFoodPool(List.of(FOOD_FAR, FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food2"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent(1, 0, List.of(), null), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food2"));
    }

    @Test
    @DisplayName("mood나 companion이 있으면 벡터 리랭킹 순서로 후보를 고른다 — 거리순 1위가 아니라 리랭킹 1위가 선택된다")
    void generate_usesRerankOrder_whenMoodPresent() {
        // given — 거리순 풀은 [food1, food2], 리랭킹이 food2를 앞세움
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotReranker.prepare("로맨틱 연인 여행")).willReturn(ranker);
        given(ranker.rerank(List.of("food1", "food2"))).willReturn(List.of("food2", "food1"));
        given(spotLookupPort.findFoodPrices(List.of("food2"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent(1, 0, List.of("로맨틱"), "연인"), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food2"));
        assertThat(saved.getTotalCost()).isNull(); // 가격 정보 없음 → null
    }

    @Test
    @DisplayName("리랭킹 준비(임베딩)는 생성 1회당 1번만 하고, 같은 랭커를 카테고리 그룹마다 재사용한다")
    void generate_preparesRerankerOnce_andReusesAcrossGroups() {
        // given — FD 그룹과 관광 그룹 둘 다 리랭킹 대상
        stubFoodPool(List.of(FOOD_NEAR));
        stubAttractionPool(List.of(NATURE));
        given(spotReranker.prepare(anyString())).willReturn(ranker);
        given(ranker.rerank(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        service.generate(1L, intent(1, 1, List.of("감성적인"), null), "AI_CHAT");

        // then
        verify(spotReranker, times(1)).prepare("감성적인");
        verify(ranker).rerank(List.of("food1"));
        verify(ranker).rerank(List.of("nature1"));
    }

    @Test
    @DisplayName("좌표가 없는 스팟은 후보에서 제외되어 코스에 담기지 않는다")
    void generate_excludesSpotWithoutCoordinate() {
        // given — 2개를 요청했지만 유효 좌표는 1개뿐
        stubFoodPool(List.of(NO_COORD, FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent(2, 0, List.of(), null), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food1"));
    }

    @Test
    @DisplayName("리랭킹 결과에 빠진 후보(임베딩 미적재)는 버리지 않고 거리순으로 뒤에 이어 붙여 요청 개수를 채운다")
    void generate_fillsFromPoolOrder_whenRerankResultIsShort() {
        // given — 풀은 [food1, food2], 리랭킹은 food2 하나만 돌려줌. 2개를 요청했으니 food1이 뒤에 붙어야 한다
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotReranker.prepare("로맨틱 연인 여행")).willReturn(ranker);
        given(ranker.rerank(List.of("food1", "food2"))).willReturn(List.of("food2"));
        given(spotLookupPort.findFoodPrices(anyList())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent(2, 0, List.of("로맨틱"), "연인"), "AI_CHAT");

        // then — 둘 다 담긴다 (방문 순서는 최근접 탐욕이 정한다)
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef)
                .containsExactlyInAnyOrder(PlaceRef.spot("food2"), PlaceRef.spot("food1"));
    }

    @Test
    @DisplayName("반경 안에 후보가 하나도 없으면 빈 코스를 저장하지 않고 NO_SPOT_CANDIDATES로 거절한다")
    void generate_throws_whenNoCandidates() {
        // given
        stubFoodPool(List.of());
        stubAttractionPool(List.of());

        // when / then
        assertThatThrownBy(() -> service.generate(1L, intent(1, 1, List.of(), null), "AI_CHAT"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.NO_SPOT_CANDIDATES);

        verify(courseRepository, never()).save(any());
        verifyNoInteractions(courseContentWriter);
    }

    @Test
    @DisplayName("같은 스팟이 여러 카테고리 풀에 나와도 코스에는 한 번만 담긴다")
    void generate_deduplicatesAcrossGroups() {
        // given — FD와 NA 풀에 같은 contentId
        SpotCandidate asFood = spot("dup", "FD", 35.1540, 129.1190);
        SpotCandidate asNature = spot("dup", "NA", 35.1540, 129.1190);
        stubFoodPool(List.of(asFood));
        stubAttractionPool(List.of(asNature));
        given(spotLookupPort.findFoodPrices(List.of("dup"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent(1, 1, List.of(), null), "AI_CHAT");

        // then
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getPlaceRef()).isEqualTo(PlaceRef.spot("dup"));
    }

    @Test
    @DisplayName("가격 조회는 FD 카테고리 아이템의 contentId만으로 호출한다")
    void generate_queriesPricesOnlyForFood() {
        // given
        stubFoodPool(List.of(FOOD_NEAR));
        stubAttractionPool(List.of(NATURE));
        given(spotLookupPort.findFoodPrices(anyList())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        service.generate(1L, intent(1, 1, List.of(), null), "AI_CHAT");

        // then — nature1은 포함되지 않는다
        verify(spotLookupPort).findFoodPrices(List.of("food1"));
    }

    // ── 사용자가 이름으로 지정한 포함·제외 스팟 ────────────────────────────────

    @Test
    @DisplayName("꼭 넣어달라고 한 스팟은 이름으로 찾아 코스에 담고, 그 카테고리 그룹의 슬롯을 하나 차지한다")
    void generate_includesPinnedSpot_andItTakesAGroupSlot() {
        // given — 관광지 1곳을 원하는데 광안리 해수욕장을 지정 → 관광지 그룹은 조회조차 하지 않는다
        given(spotLookupPort.findActiveByTitleNear(eq("광안리 해수욕장"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of(BEACH));
        given(spotLookupPort.findActiveByIds(List.of("beach"))).willReturn(Map.of("beach", BEACH));
        stubFoodPool(List.of(FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithPins(1, 1, List.of("광안리 해수욕장"), List.of()), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef)
                .containsExactlyInAnyOrder(PlaceRef.spot("beach"), PlaceRef.spot("food1"));
        verify(spotLookupPort, never()).findNearbyByCategories(eq(ATTRACTION_GROUP), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("지정 스팟이 원하는 개수보다 많아도 전부 담고, 남은 슬롯이 없으면 그 그룹은 추가로 고르지 않는다")
    void generate_pinnedSpotsCanExceedGroupCount() {
        // given — 관광지 1곳 요청 + 관광지 2곳 지정
        given(spotLookupPort.findActiveByTitleNear(eq("광안리 해수욕장"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of(BEACH));
        given(spotLookupPort.findActiveByTitleNear(eq("해운대 해수욕장"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of(HAEUNDAE));
        given(spotLookupPort.findActiveByIds(List.of("beach", "haeundae"))).willReturn(Map.of("beach", BEACH, "haeundae", HAEUNDAE));
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithPins(0, 1, List.of("광안리 해수욕장", "해운대 해수욕장"), List.of()), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef)
                .containsExactlyInAnyOrder(PlaceRef.spot("beach"), PlaceRef.spot("haeundae"));
        verify(spotLookupPort, never()).findNearbyByCategories(anyList(), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("빼달라고 한 스팟은 이름으로 찾아 후보 풀에서 걸러낸다 — 거리순 1위여도 선택되지 않는다")
    void generate_excludesSpotByName() {
        // given — 풀은 [food1, food2], food1(=이름으로 찾은 스팟)을 제외 → food2 선택
        given(spotLookupPort.findActiveByTitleNear(eq("food1명"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of(FOOD_NEAR));
        given(spotLookupPort.findActiveByIds(List.of("food1"))).willReturn(Map.of("food1", FOOD_NEAR));
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotLookupPort.findFoodPrices(List.of("food2"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithPins(1, 0, List.of(), List.of("food1명")), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food2"));
    }

    @Test
    @DisplayName("이름에 맞는 스팟이 없으면 그 지정은 건너뛰고 나머지로 정상 생성한다")
    void generate_skipsUnresolvedPinnedName() {
        // given
        given(spotLookupPort.findActiveByTitleNear(eq("없는 장소"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of());
        given(spotLookupPort.findActiveByTitleNear(eq("또 없는 장소"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of());
        stubFoodPool(List.of(FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithPins(1, 0, List.of("없는 장소"), List.of("또 없는 장소")), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food1"));
    }

    @Test
    @DisplayName("같은 스팟이 포함과 제외 양쪽으로 풀리면 제외가 이긴다 (이름은 달라도 같은 스팟인 경우)")
    void generate_excludeWins_whenSameSpotResolvedOnBothSides() {
        // given — "광안리 해수욕장"과 "광안리해변"이 같은 스팟으로 풀림
        given(spotLookupPort.findActiveByTitleNear(eq("광안리 해수욕장"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of(BEACH));
        given(spotLookupPort.findActiveByTitleNear(eq("광안리해변"), eq(GWANGAN_LAT), eq(GWANGAN_LON), anyInt())).willReturn(List.of(BEACH));
        given(spotLookupPort.findActiveByIds(List.of("beach"))).willReturn(Map.of("beach", BEACH));
        stubAttractionPool(List.of(NATURE));
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithPins(0, 1, List.of("광안리 해수욕장"), List.of("광안리해변")), "AI_CHAT");

        // then — beach는 빠지고 관광지 슬롯은 풀에서 채운다
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("nature1"));
    }

    @Test
    @DisplayName("확인 단계에서 이미 매칭된 지정은 contentId로 일괄 조회하고 이름 검색을 다시 하지 않는다")
    void generate_usesStoredContentId_forResolvedPins() {
        // given — 포함·제외 모두 해결된 pin
        given(spotLookupPort.findActiveByIds(List.of("beach"))).willReturn(Map.of("beach", BEACH));
        given(spotLookupPort.findActiveByIds(List.of("food1"))).willReturn(Map.of("food1", FOOD_NEAR));
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotLookupPort.findFoodPrices(List.of("food2"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithResolvedPins(1, 1,
                List.of(new SpotPin("광안리 바다", "beach", "광안리해수욕장")),
                List.of(new SpotPin("food1명", "food1", "food1명"))), "AI_CHAT");

        // then — beach 포함, food1 제외 → food2
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef)
                .containsExactlyInAnyOrder(PlaceRef.spot("beach"), PlaceRef.spot("food2"));
        verify(spotLookupPort, never()).findActiveByTitleNear(anyString(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("매칭해 둔 스팟이 그 사이 비활성화됐으면 건너뛰고 그 슬롯은 후보 풀에서 채운다")
    void generate_skipsResolvedPin_whenNoLongerActive() {
        // given
        given(spotLookupPort.findActiveByIds(List.of("beach"))).willReturn(Map.of());
        stubAttractionPool(List.of(NATURE));
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithResolvedPins(0, 1,
                List.of(new SpotPin("광안리 바다", "beach", "광안리해수욕장")), List.of()), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("nature1"));
    }

    // ── 기준점 ("X 근처") ────────────────────────────────────────────────────

    @Test
    @DisplayName("기준점이 찾아져 있으면 지역 중심이 아니라 기준점 좌표를 검색 중심으로 쓴다")
    void generate_usesAnchorPoint_asSearchCenter() {
        // given — 광안리로 분류된 intent지만 기준점(행사장)은 해운대 쪽 좌표
        GeoPoint venue = new GeoPoint(35.1587, 129.1604);
        CourseAnchor anchor = CourseAnchor.of("부산국제항만컨퍼런스")
                .resolvedTo(CourseAnchor.AnchorSource.EVENT, "ev1", "부산국제항만컨퍼런스", venue, "10.14~10.16");
        CourseIntent intent = new CourseIntent("광안리", false, null, null, List.of(),
                new SlotHints(1, 0, 0, 0), false, List.of(), List.of(), anchor);
        given(spotLookupPort.findNearbyByCategories(eq(FOOD_GROUP), eq(venue.latitude()), eq(venue.longitude()), anyDouble(), anyInt()))
                .willReturn(List.of(FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent, "AI_CHAT");

        // then — 후보 조회 중심이 기준점 좌표이고, 첫 구간 거리도 기준점 기준이다
        verify(spotLookupPort).findNearbyByCategories(FOOD_GROUP, venue.latitude(), venue.longitude(),
                CourseGenerationService.SEARCH_RADIUS_M, POOL_SIZE);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.startPoint()).isEqualTo(venue);
    }

    // ── 폼 경로 옵션 ─────────────────────────────────────────────────────────

    private static CourseIntent intentWithBudget(Integer budget, SlotHints hints) {
        return new CourseIntent("광안리", false, budget, null, List.of(), hints, false);
    }

    @Test
    @DisplayName("폼 옵션은 동행이 있어도 리랭킹을 타지 않고, Gemini 대신 템플릿 제목·빈 소개로 ALGORITHM 코스를 저장한다")
    void generate_form_usesTemplateTitle_withoutAiCalls() {
        // given — 동행이 있어 챗봇이라면 임베딩을 했을 intent
        CourseIntent intent = new CourseIntent("광안리", false, null, "연인", List.of(), new SlotHints(1, 0, 0, 0), false);
        stubFoodPool(List.of(FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubSaveReturnsArgument();

        // when
        GenerationResult result = service.generate(1L, intent, GenerationOptions.form("광안리 중심 코스"));

        // then
        Course saved = result.course();
        assertThat(saved.getGenerationMode()).isEqualTo(GenerationMode.ALGORITHM);
        assertThat(saved.getTitle()).isEqualTo("광안리 중심 코스");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getCreatedBy()).isEqualTo("FORM");
        assertThat(result.budgetFilterRelaxed()).isFalse();
        verifyNoInteractions(spotReranker, courseContentWriter);
    }

    // ── 예산 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("예산 등급의 1곳 상한을 넘는 식사 후보는 거른다 — 가격 정보가 없는 후보는 통과")
    void generate_appliesBudgetCap_toFoodPool() {
        // given — 3만원 등급(상한 12,000원). 풀은 [food1 15,000원, food2 8,000원], 식사 1곳
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotLookupPort.findFoodPrices(List.of("food1", "food2"))).willReturn(Map.of("food1", 15000, "food2", 8000));
        given(spotLookupPort.findFoodPrices(List.of("food2"))).willReturn(Map.of("food2", 8000));
        stubContent();
        stubSaveReturnsArgument();

        // when
        GenerationResult result = service.generate(1L, intentWithBudget(30000, new SlotHints(1, 0, 0, 0)), GenerationOptions.aiChat());

        // then
        assertThat(result.course().getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food2"));
        assertThat(result.course().getTotalCost()).isEqualTo(8000);
        assertThat(result.budgetFilterRelaxed()).isFalse();
    }

    @Test
    @DisplayName("상한 안의 식사 후보가 슬롯보다 적으면 필터를 풀고 채우며 결과에 표시한다")
    void generate_relaxesBudgetFilter_whenNotEnoughAffordable() {
        // given — 3만원 등급, 풀 전부가 상한 초과
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotLookupPort.findFoodPrices(List.of("food1", "food2"))).willReturn(Map.of("food1", 15000, "food2", 20000));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of("food1", 15000));
        stubContent();
        stubSaveReturnsArgument();

        // when
        GenerationResult result = service.generate(1L, intentWithBudget(30000, new SlotHints(1, 0, 0, 0)), GenerationOptions.aiChat());

        // then — 거리순 1위(food1)가 그대로 들어간다
        assertThat(result.course().getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food1"));
        assertThat(result.budgetFilterRelaxed()).isTrue();
    }

    @Test
    @DisplayName("무지출 등급은 식사·카페 그룹을 조회조차 하지 않고 관광지만 담는다")
    void generate_noneBudget_skipsFoodGroup() {
        // given — 예산 0원, 슬롯 힌트 없음 → 등급 기본값(관광 4)
        stubAttractionPool(List.of(NATURE));
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intentWithBudget(0, null), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("nature1"));
        verify(spotLookupPort, never()).findNearbyByCategories(eq(FOOD_GROUP), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("꼭 포함하는 스팟은 예산 상한과 무관하게 담긴다 — 사용자가 명시한 것이므로")
    void generate_pinnedSpot_ignoresBudgetCap() {
        // given — 1만원 등급(식사 1, 상한 10,000원)인데 50,000원짜리 식당을 지정
        CourseIntent intent = new CourseIntent("광안리", false, 10000, null, List.of(), new SlotHints(null, null, 0, null), false,
                List.of(new SpotPin("food1명", "food1", "food1명")), List.of());
        given(spotLookupPort.findActiveByIds(List.of("food1"))).willReturn(Map.of("food1", FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of("food1", 50000));
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent, "AI_CHAT");

        // then — 지정 식당이 식사 슬롯을 차지해 FD 풀은 조회하지 않고, 비용은 그대로 반영된다
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food1"));
        assertThat(saved.getTotalCost()).isEqualTo(50000);
        verify(spotLookupPort, never()).findNearbyByCategories(eq(FOOD_GROUP), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    // ── 다양성 (샘플링·세부 분류·최근 이력) ────────────────────────────────────

    /** 난수가 항상 1에 가까운 생성기 — 선택기가 남은 후보 중 마지막(최하위)을 뽑는다 */
    private static final RandomGenerator ALWAYS_LAST = () -> -1L;

    private static List<SpotCandidate> tenAttractions() {
        return java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> spot("na" + i, "NA", 35.1540 + i * 0.001, 129.1190))
                .toList();
    }

    @Test
    @DisplayName("폼 모드도 같은 입력에서 시드가 다르면 다른 스팟 조합이 나온다 — 앞에서 count개를 자르지 않는다")
    void generate_form_variesAcrossSeeds() {
        // given — 관광지 풀 10곳, 3곳 고름. 시드 20개로 돌려 조합이 하나뿐이지 않은지 본다
        stubAttractionPool(tenAttractions());
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubSaveReturnsArgument();
        CourseIntent intent = new CourseIntent("광안리", false, null, null, List.of(), new SlotHints(0, 0, 3, 0), false);

        // when
        Set<Set<PlaceRef>> combos = new HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            Course saved = serviceWith(new Random(seed))
                    .generate(1L, intent, GenerationOptions.form("광안리 중심 코스")).course();
            assertThat(saved.getItems()).hasSize(3);
            combos.add(saved.getItems().stream().map(CourseItem::getPlaceRef).collect(Collectors.toSet()));
        }

        // then
        assertThat(combos.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("예산 필터는 샘플링 전에 걸린다 — 난수가 어떻든 상한을 넘는 식당은 뽑히지 않는다")
    void generate_appliesBudgetCap_beforeSampling() {
        // given — 3만원 등급(상한 12,000원). 풀 [food1 15,000 · food2 8,000 · food3 가격 없음], 식사 1곳.
        //         ALWAYS_LAST는 남은 후보 중 마지막을 뽑으므로 필터가 없다면 food1이 아니라도 마지막 후보가 뽑힌다
        SpotCandidate food3 = spot("food3", "FD", 35.1600, 129.1300);
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR, food3));
        given(spotLookupPort.findFoodPrices(List.of("food1", "food2", "food3"))).willReturn(Map.of("food1", 15000, "food2", 8000));
        given(spotLookupPort.findFoodPrices(List.of("food3"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = serviceWith(ALWAYS_LAST)
                .generate(1L, intentWithBudget(30000, new SlotHints(1, 0, 0, 0)), "AI_CHAT");

        // then — 상한 안 후보 [food2, food3] 중 마지막인 food3. food1은 후보에서 빠져 있다
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food3"));
    }

    @Test
    @DisplayName("같은 세부 분류(lcls_systm_2)는 1곳까지만 먼저 고른다 — 관광지 3곳이 해수욕장 3개로 채워지지 않는다")
    void generate_limitsSameSubCategory() {
        // given — 거리순 [해수욕장A, 해수욕장B, 공원, 해수욕장C], 관광지 2곳. 최상위부터 고르되 같은 세부 분류는 건너뛴다
        SpotCandidate beachA = new SpotCandidate("beachA", "해수욕장A", null, "NA", "NA",
                BigDecimal.valueOf(129.1190), BigDecimal.valueOf(35.1540), "NA01");
        SpotCandidate beachB = new SpotCandidate("beachB", "해수욕장B", null, "NA", "NA",
                BigDecimal.valueOf(129.1191), BigDecimal.valueOf(35.1541), "NA01");
        SpotCandidate park = new SpotCandidate("park", "공원", null, "NA", "NA",
                BigDecimal.valueOf(129.1192), BigDecimal.valueOf(35.1542), "NA02");
        SpotCandidate beachC = new SpotCandidate("beachC", "해수욕장C", null, "NA", "NA",
                BigDecimal.valueOf(129.1193), BigDecimal.valueOf(35.1543), "NA01");
        stubAttractionPool(List.of(beachA, beachB, park, beachC));
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent(0, 2, List.of(), null), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef)
                .containsExactlyInAnyOrder(PlaceRef.spot("beachA"), PlaceRef.spot("park"));
    }

    @Test
    @DisplayName("최근 코스에 담겼던 스팟은 이력을 userId로 1회 조회해 감점한다 — 감점 때문에 2위가 1위를 이긴다")
    void generate_penalizesRecentlyUsedSpots() {
        // given — 거리순 [food1(최근 이력), food2], 식사 1곳. 난수 0.5:
        //   감점 없으면 가중치 [1, 0.88] → r = 0.94 → food1. 감점 후 [0.2, 0.88] → r = 0.54 → food2
        RandomGenerator half = () -> Long.MIN_VALUE; // nextDouble() == 0.5
        given(courseRepository.findRecentSpotContentIds(1L, CourseGenerationService.RECENT_HISTORY_COURSES))
                .willReturn(Set.of("food1"));
        stubFoodPool(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotLookupPort.findFoodPrices(List.of("food2"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = serviceWith(half).generate(1L, intent(1, 0, List.of(), null), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food2"));
        verify(courseRepository, times(1)).findRecentSpotContentIds(1L, CourseGenerationService.RECENT_HISTORY_COURSES);
    }

    @Test
    @DisplayName("꼭 포함하는 스팟은 샘플링과 무관하게 항상 담긴다")
    void generate_pinnedSpot_alwaysIncluded_regardlessOfSampling() {
        // given — 관광지 3곳 중 1곳은 지정(beach). 나머지 2곳은 풀 10개에서 샘플링
        CourseIntent intent = new CourseIntent("광안리", false, null, null, List.of(), new SlotHints(0, 0, 3, 0), false,
                List.of(new SpotPin("광안리해수욕장", "beach", "광안리해수욕장")), List.of());
        given(spotLookupPort.findActiveByIds(List.of("beach"))).willReturn(Map.of("beach", BEACH));
        stubAttractionPool(tenAttractions());
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubSaveReturnsArgument();

        // when / then
        for (long seed = 0; seed < 10; seed++) {
            Course saved = serviceWith(new Random(seed))
                    .generate(1L, intent, GenerationOptions.form("광안리 중심 코스")).course();
            assertThat(saved.getItems()).hasSize(3)
                    .extracting(CourseItem::getPlaceRef).contains(PlaceRef.spot("beach"));
        }
    }
}
