package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
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
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourseGenerationServiceTest {

    @Mock SpotLookupPort spotLookupPort;
    @Mock SpotReranker spotReranker;
    @Mock CourseContentWriter courseContentWriter;
    @Mock CourseRepository courseRepository;

    @InjectMocks CourseGenerationService service;

    // 픽스처 — 광안리 중심(35.1531, 129.1187) 기준. SpotCandidate는 mapX=경도, mapY=위도.

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
        return new CourseIntent("광안리", false, null, companion, mood,
                new SlotHints(food, 0, attraction, 0), false);
    }

    private void stubSaveReturnsArgument() {
        given(courseRepository.save(any(Course.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private void stubContent() {
        given(courseContentWriter.generate(any(CourseIntent.class), anyList()))
                .willReturn(new CourseContent("생성된 제목", "생성된 소개"));
    }

    private void stubAttractionPools(List<SpotCandidate> na) {
        given(spotLookupPort.findNearbyByCategory(eq("NA"), anyDouble(), anyDouble())).willReturn(na);
        given(spotLookupPort.findNearbyByCategory(eq("HS"), anyDouble(), anyDouble())).willReturn(List.of());
        given(spotLookupPort.findNearbyByCategory(eq("VE"), anyDouble(), anyDouble())).willReturn(List.of());
    }

    @Test
    @DisplayName("스팟을 최근접 순으로 조립해 SPOT 참조 아이템으로 저장하고, FD만 가격을 붙여 총비용을 계산한다")
    void generate_assemblesAndSaves() {
        // given
        given(spotLookupPort.findNearbyByCategory(eq("FD"), anyDouble(), anyDouble())).willReturn(List.of(FOOD_NEAR));
        stubAttractionPools(List.of(NATURE));
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
        verify(spotReranker, never()).rerank(anyString(), anyList());
    }

    @Test
    @DisplayName("지원하지 않는 지역이면 UNKNOWN_START_AREA를 던지고 아무것도 저장하지 않는다")
    void generate_throws_whenStartAreaUnknown() {
        CourseIntent unknown = new CourseIntent("화성", false, null, null, List.of(), null, false);

        assertThatThrownBy(() -> service.generate(1L, unknown, "AI_CHAT"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.UNKNOWN_START_AREA);

        verifyNoInteractions(spotLookupPort, courseRepository, courseContentWriter);
    }

    @Test
    @DisplayName("mood나 companion이 있으면 벡터 리랭킹 순서로 후보를 고른다 — 거리순 1위가 아니라 리랭킹 1위가 선택된다")
    void generate_usesRerankOrder_whenMoodPresent() {
        // given — 거리순 풀은 [food1, food2], 리랭킹이 food2를 앞세움
        given(spotLookupPort.findNearbyByCategory(eq("FD"), anyDouble(), anyDouble())).willReturn(List.of(FOOD_NEAR, FOOD_FAR));
        given(spotReranker.rerank(eq("로맨틱 연인 여행"), eq(List.of("food1", "food2")))).willReturn(List.of("food2", "food1"));
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
    @DisplayName("좌표가 없는 스팟은 후보에서 제외되어 코스에 담기지 않는다")
    void generate_excludesSpotWithoutCoordinate() {
        // given — 2개를 요청했지만 유효 좌표는 1개뿐
        given(spotLookupPort.findNearbyByCategory(eq("FD"), anyDouble(), anyDouble())).willReturn(List.of(NO_COORD, FOOD_NEAR));
        given(spotLookupPort.findFoodPrices(List.of("food1"))).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        Course saved = service.generate(1L, intent(2, 0, List.of(), null), "AI_CHAT");

        // then
        assertThat(saved.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("food1"));
    }

    @Test
    @DisplayName("같은 스팟이 여러 카테고리 풀에 나와도 코스에는 한 번만 담긴다")
    void generate_deduplicatesAcrossGroups() {
        // given — FD와 NA 풀에 같은 contentId
        SpotCandidate asFood = spot("dup", "FD", 35.1540, 129.1190);
        SpotCandidate asNature = spot("dup", "NA", 35.1540, 129.1190);
        given(spotLookupPort.findNearbyByCategory(eq("FD"), anyDouble(), anyDouble())).willReturn(List.of(asFood));
        stubAttractionPools(List.of(asNature));
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
        given(spotLookupPort.findNearbyByCategory(eq("FD"), anyDouble(), anyDouble())).willReturn(List.of(FOOD_NEAR));
        stubAttractionPools(List.of(NATURE));
        given(spotLookupPort.findFoodPrices(anyList())).willReturn(Map.of());
        stubContent();
        stubSaveReturnsArgument();

        // when
        service.generate(1L, intent(1, 1, List.of(), null), "AI_CHAT");

        // then — nature1은 포함되지 않는다
        verify(spotLookupPort).findFoodPrices(List.of("food1"));
    }
}
