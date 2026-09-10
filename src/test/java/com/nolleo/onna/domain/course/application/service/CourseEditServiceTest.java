package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseItemsCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseItemResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaceType;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseEditServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock SpotLookupPort spotLookupPort;

    @InjectMocks CourseEditService service;

    // 픽스처 — 광안리 중심(35.1531, 129.1187). SpotCandidate는 mapX=경도, mapY=위도.
    private static final Long OWNER = 1L;
    private static final Long COURSE_ID = 10L;
    private static final CourseIntent INTENT =
            new CourseIntent("광안리", false, null, null, List.of(), null, false);

    private static final SpotCandidate A = spot("A", "NA", 35.1540, 129.1190);
    private static final SpotCandidate B = spot("B", "HS", 35.1600, 129.1250);
    private static final SpotCandidate C = spot("C", "VE", 35.1650, 129.1300);
    private static final SpotCandidate X_FOOD = spot("X", "FD", 35.1580, 129.1220);
    private static final SpotCandidate NO_COORD = new SpotCandidate("ghost", "유령", null, "NA", "NA", null, null);

    private static SpotCandidate spot(String id, String category, double lat, double lon) {
        return new SpotCandidate(id, id + "명", null, category, category,
                BigDecimal.valueOf(lon), BigDecimal.valueOf(lat));
    }

    /** 기존 아이템이 순번 1..n으로 담긴 저장된 코스 */
    private static Course savedCourse(Long userId, CourseIntent intent, String... spotIds) {
        List<CourseItem> items = IntStream.range(0, spotIds.length)
                .mapToObj(i -> CourseItem.restore((long) i + 1, COURSE_ID, (short) (i + 1),
                        PlaceRef.spot(spotIds[i]), null, 100))
                .toList();
        return Course.restore(COURSE_ID, userId, UUID.randomUUID(), GenerationMode.AI, null,
                "제목", "소개", intent, null, ShareInfo.initial(), items,
                OffsetDateTime.now(), "AI_CHAT");
    }

    private static UpdateCourseItemsCommand command(Long userId, PlaceRef... refs) {
        return new UpdateCourseItemsCommand(COURSE_ID, userId, List.of(refs));
    }

    private static UpdateCourseItemsCommand spots(String... ids) {
        return command(OWNER, java.util.Arrays.stream(ids).map(PlaceRef::spot).toArray(PlaceRef[]::new));
    }

    private void stubOwnerCourse(String... existing) {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(savedCourse(OWNER, INTENT, existing)));
    }

    private void stubUpdateReturnsArgument() {
        given(courseRepository.saveReplacedItems(any(Course.class), eq(String.valueOf(OWNER))))
                .willAnswer(inv -> inv.getArgument(0));
    }

    private static Map<String, SpotCandidate> byId(SpotCandidate... spots) {
        return java.util.Arrays.stream(spots)
                .collect(java.util.stream.Collectors.toMap(SpotCandidate::contentId, s -> s));
    }

    // ── 정상 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("이동·삭제·추가가 섞인 최종 리스트를 받으면 배열 순서대로 순번을 다시 매기고 거리·비용을 재계산해 저장한다")
    void updateItems_replacesInGivenOrder() {
        // given — 기존 [A,B,C] → 최종 [C, X(음식점), A]: 1번을 3번으로, B 삭제, X 추가
        stubOwnerCourse("A", "B", "C");
        given(spotLookupPort.findActiveByIds(List.of("C", "X", "A"))).willReturn(byId(A, C, X_FOOD));
        given(spotLookupPort.findFoodPrices(List.of("X"))).willReturn(Map.of("X", 15000));
        stubUpdateReturnsArgument();

        // when
        CourseResponse response = service.updateItems(spots("C", "X", "A"));

        // then — 저장된 애그리거트
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).saveReplacedItems(captor.capture(), eq("1"));
        verify(spotLookupPort, never()).findByIds(anyList()); // 쓰기 경로는 활성 스팟만 조회한다
        List<CourseItem> items = captor.getValue().getItems();

        assertThat(items).extracting(CourseItem::getPlaceRef)
                .containsExactly(PlaceRef.spot("C"), PlaceRef.spot("X"), PlaceRef.spot("A"));
        assertThat(items).extracting(CourseItem::getSerialNum).containsExactly((short) 1, (short) 2, (short) 3);
        assertThat(items.get(1).getExpectedCost()).isEqualTo(15000);
        assertThat(items.get(0).getExpectedCost()).isNull();
        assertThat(captor.getValue().getTotalCost()).isEqualTo(15000);

        // 1번은 지역 중심 기준, 이후는 직전 지점 기준 — 순서를 재배치하지 않았음을 거리로도 확인
        int expectedFirst = (int) Math.round(CourseAssembler.distanceMeters(35.1531, 129.1187, 35.1650, 129.1300));
        int expectedSecond = (int) Math.round(CourseAssembler.distanceMeters(35.1650, 129.1300, 35.1580, 129.1220));
        assertThat(items.get(0).getDistanceFromPrevM()).isEqualTo(expectedFirst);
        assertThat(items.get(1).getDistanceFromPrevM()).isEqualTo(expectedSecond);

        // then — 응답에는 스팟 상세가 병합되어 있다
        assertThat(response.items()).extracting(CourseItemResponse::originalId).containsExactly("C", "X", "A");
        assertThat(response.items()).extracting(CourseItemResponse::title).containsExactly("C명", "X명", "A명");
        assertThat(response.items()).extracting(CourseItemResponse::placeType).containsOnly("SPOT");
    }

    @Test
    @DisplayName("같은 리스트를 다시 보내면 결과가 동일하다 (멱등)")
    void updateItems_isIdempotent() {
        stubOwnerCourse("A", "B");
        given(spotLookupPort.findActiveByIds(List.of("A", "B"))).willReturn(byId(A, B));
        given(spotLookupPort.findFoodPrices(List.of())).willReturn(Map.of());
        stubUpdateReturnsArgument();

        CourseResponse response = service.updateItems(spots("A", "B"));

        assertThat(response.items()).extracting(CourseItemResponse::originalId).containsExactly("A", "B");
        assertThat(response.items()).extracting(CourseItemResponse::serialNum).containsExactly((short) 1, (short) 2);
        assertThat(response.totalCost()).isNull();
    }

    // ── 거부 — 거부 시 저장이 호출되지 않아 코스는 변경 전 상태를 유지한다 ─────

    @Test
    @DisplayName("없는 코스면 COURSE_NOT_FOUND")
    void updateItems_throws_whenCourseNotFound() {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItems(spots("A")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_NOT_FOUND);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("남의 코스면 COURSE_ACCESS_DENIED — IDOR 차단")
    void updateItems_throws_whenNotOwner() {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(savedCourse(OWNER, INTENT, "A")));

        assertThatThrownBy(() -> service.updateItems(command(99L, PlaceRef.spot("A"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ACCESS_DENIED);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("빈 배열이면 COURSE_ITEM_EMPTY")
    void updateItems_throws_whenEmpty() {
        stubOwnerCourse("A");

        assertThatThrownBy(() -> service.updateItems(command(OWNER)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_EMPTY);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("16개를 보내면 COURSE_ITEM_LIMIT_EXCEEDED")
    void updateItems_throws_whenOverLimit() {
        stubOwnerCourse("A");
        String[] sixteen = IntStream.rangeClosed(1, CoursePlaces.MAX_ITEMS + 1).mapToObj(i -> "S" + i).toArray(String[]::new);

        assertThatThrownBy(() -> service.updateItems(spots(sixteen)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_LIMIT_EXCEEDED);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("같은 장소가 두 번 있으면 COURSE_ITEM_DUPLICATED")
    void updateItems_throws_whenDuplicated() {
        stubOwnerCourse("A");

        assertThatThrownBy(() -> service.updateItems(spots("A", "B", "A")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_DUPLICATED);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("FOOD 타입이 섞여 있으면 COURSE_PLACE_TYPE_NOT_SUPPORTED — 이번 범위는 SPOT만")
    void updateItems_throws_whenFoodType() {
        stubOwnerCourse("A");
        PlaceRef food = new PlaceRef(CoursePlaceType.FOOD, "123");

        assertThatThrownBy(() -> service.updateItems(command(OWNER, PlaceRef.spot("A"), food)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_PLACE_TYPE_NOT_SUPPORTED);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("존재하지 않거나 비활성인 스팟이 하나라도 있으면 COURSE_PLACE_NOT_FOUND")
    void updateItems_throws_whenSpotMissing() {
        stubOwnerCourse("A");
        given(spotLookupPort.findActiveByIds(List.of("A", "Z"))).willReturn(byId(A)); // Z는 없거나 비활성 — 활성 조회 결과에서 빠진다

        assertThatThrownBy(() -> service.updateItems(spots("A", "Z")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_PLACE_NOT_FOUND);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("좌표가 없는 스팟은 거리 계산이 불가하므로 COURSE_PLACE_NOT_FOUND")
    void updateItems_throws_whenSpotHasNoCoordinate() {
        stubOwnerCourse("A");
        given(spotLookupPort.findActiveByIds(List.of("A", "ghost"))).willReturn(byId(A, NO_COORD));

        assertThatThrownBy(() -> service.updateItems(spots("A", "ghost")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_PLACE_NOT_FOUND);
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }

    @Test
    @DisplayName("코스의 시작 지역을 해석할 수 없으면 UNKNOWN_START_AREA — 1번 거리 기준점을 잡을 수 없다")
    void updateItems_throws_whenStartAreaUnknown() {
        CourseIntent unknownArea = new CourseIntent("화성", false, null, null, List.of(), null, false);
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(savedCourse(OWNER, unknownArea, "A")));

        assertThatThrownBy(() -> service.updateItems(spots("A")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.UNKNOWN_START_AREA);
        verify(spotLookupPort, never()).findActiveByIds(anyList());
        verify(courseRepository, never()).saveReplacedItems(any(), anyString());
    }
}
