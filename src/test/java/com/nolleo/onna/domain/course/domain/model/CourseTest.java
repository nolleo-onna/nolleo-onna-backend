package com.nolleo.onna.domain.course.domain.model;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.CourseType;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseTest {

    private static final CourseIntent INTENT =
            new CourseIntent("광안리", false, null, null, List.of(), null, false);

    private static Course aiCourse() {
        return Course.createByAi(1L, UUID.randomUUID(), INTENT, "AI_CHAT");
    }

    @Test
    @DisplayName("AI 코스는 courseType 없이 생성되고 초기 상태는 비공개·좋아요 0이다")
    void createByAi_initialState() {
        Course course = aiCourse();

        assertThat(course.getGenerationMode()).isEqualTo(GenerationMode.AI);
        assertThat(course.getCourseType()).isNull();
        assertThat(course.getItems()).isEmpty();
        assertThat(course.getTotalCost()).isNull();
        assertThat(course.getShareInfo().isPublic()).isFalse();
        assertThat(course.getShareInfo().likeCount()).isZero();
    }

    @Test
    @DisplayName("userId가 없거나 startArea가 확정되지 않은 intent로는 생성할 수 없다")
    void create_throws_whenInvalid() {
        CourseIntent noArea = new CourseIntent(null, false, null, null, List.of(), null, false);

        assertThatThrownBy(() -> Course.createByAi(null, UUID.randomUUID(), INTENT, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Course.createByAi(1L, UUID.randomUUID(), noArea, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ALGORITHM 코스는 courseType이 필수다")
    void createByAlgorithm_throws_whenCourseTypeNull() {
        assertThatThrownBy(() -> Course.createByAlgorithm(1L, UUID.randomUUID(), null, INTENT, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addItem은 호출 순서대로 1부터 순번을 매기고 PlaceRef를 그대로 보관한다")
    void addItem_assignsSerialInOrder() {
        Course course = aiCourse();

        course.addItem(PlaceRef.spot("A"), null, 100);
        course.addItem(PlaceRef.spot("B"), null, 200);
        course.addItem(PlaceRef.spot("C"), null, 300);

        List<CourseItem> items = course.getItems();
        assertThat(items).extracting(CourseItem::getSerialNum).containsExactly((short) 1, (short) 2, (short) 3);
        assertThat(items).extracting(CourseItem::getPlaceRef)
                .containsExactly(PlaceRef.spot("A"), PlaceRef.spot("B"), PlaceRef.spot("C"));
        assertThat(items).extracting(CourseItem::getDistanceFromPrevM).containsExactly(100, 200, 300);
    }

    @Test
    @DisplayName("totalCost는 expectedCost가 있는 아이템만 합산하고, 하나도 없으면 null이다")
    void addItem_recomputesTotalCost() {
        Course course = aiCourse();

        course.addItem(PlaceRef.spot("관광지"), null, 0);
        assertThat(course.getTotalCost()).isNull();

        course.addItem(PlaceRef.spot("식당1"), 12000, 0);
        course.addItem(PlaceRef.spot("식당2"), 8000, 0);
        assertThat(course.getTotalCost()).isEqualTo(20000);
    }

    @Test
    @DisplayName("getPlaceRefs는 중복을 제거한 참조 집합을 돌려준다")
    void getPlaceRefs_deduplicates() {
        Course course = aiCourse();
        course.addItem(PlaceRef.spot("A"), null, 0);
        course.addItem(PlaceRef.spot("A"), null, 0);
        course.addItem(PlaceRef.spot("B"), null, 0);

        assertThat(course.getPlaceRefs()).containsExactlyInAnyOrder(PlaceRef.spot("A"), PlaceRef.spot("B"));
    }

    @Test
    @DisplayName("getItems는 외부에서 수정할 수 없는 리스트를 반환한다")
    void getItems_isUnmodifiable() {
        Course course = aiCourse();
        course.addItem(PlaceRef.spot("A"), null, 0);

        assertThatThrownBy(() -> course.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("applyAiContent는 빈 제목을 거부한다")
    void applyAiContent_throws_whenTitleBlank() {
        Course course = aiCourse();

        assertThatThrownBy(() -> course.applyAiContent(" ", "소개"))
                .isInstanceOf(IllegalArgumentException.class);

        course.applyAiContent("광안리 데이트", "소개");
        assertThat(course.getTitle()).isEqualTo("광안리 데이트");
    }

    // ── replaceItems (코스 수정 · Full State Replacement) ─────────────────────

    private static final double START_LAT = DistrictCenter.GWANGAN.getLatitude();
    private static final double START_LON = DistrictCenter.GWANGAN.getLongitude();

    private static Course.VisitStop stop(String id, double lat, double lon, Integer expectedCost) {
        return new Course.VisitStop(PlaceRef.spot(id), lat, lon, expectedCost);
    }

    private static int meters(double lat1, double lon1, double lat2, double lon2) {
        return (int) Math.round(CourseAssembler.distanceMeters(lat1, lon1, lat2, lon2));
    }

    @Test
    @DisplayName("replaceItems는 전달된 순서대로 1부터 순번을 매기고, 인접 거리와 totalCost를 애그리거트가 계산한다")
    void replaceItems_reassignsSerial_measuresDistance_recomputesCost() {
        Course course = aiCourse();
        course.addItem(PlaceRef.spot("A"), 5000, 100);
        course.addItem(PlaceRef.spot("B"), null, 200);
        course.addItem(PlaceRef.spot("C"), null, 300);

        // 1번을 3번으로 이동 + 2번 삭제 + X 추가 — 한 번의 교체로 수렴
        course.replaceItems(List.of(
                stop("C", 35.1650, 129.1300, null),
                stop("X", 35.1580, 129.1220, 12000),
                stop("A", 35.1540, 129.1190, 5000)
        ));

        List<CourseItem> items = course.getItems();
        assertThat(items).extracting(CourseItem::getPlaceRef)
                .containsExactly(PlaceRef.spot("C"), PlaceRef.spot("X"), PlaceRef.spot("A"));
        assertThat(items).extracting(CourseItem::getSerialNum).containsExactly((short) 1, (short) 2, (short) 3);
        assertThat(course.getTotalCost()).isEqualTo(17000);

        // 첫 지점은 코스 시작 지역(광안리) 중심 기준, 이후는 직전 지점 기준 — 순서를 재배치하지 않는다
        assertThat(items).extracting(CourseItem::getDistanceFromPrevM).containsExactly(
                meters(START_LAT, START_LON, 35.1650, 129.1300),
                meters(35.1650, 129.1300, 35.1580, 129.1220),
                meters(35.1580, 129.1220, 35.1540, 129.1190));
    }

    @Test
    @DisplayName("replaceItems로 음식점을 전부 빼면 totalCost는 0이 아니라 null이 된다")
    void replaceItems_setsTotalCostNull_whenNoFood() {
        Course course = aiCourse();
        course.addItem(PlaceRef.spot("식당"), 9000, 0);

        course.replaceItems(List.of(stop("관광지", 35.1540, 129.1190, null)));

        assertThat(course.getTotalCost()).isNull();
    }

    @Test
    @DisplayName("replaceItems는 빈 목록·상한 초과·중복을 도메인 에러코드로 거부하고, 거부 시 기존 아이템을 건드리지 않는다")
    void replaceItems_rejectsInvalidStops_andKeepsItems() {
        Course course = aiCourse();
        course.addItem(PlaceRef.spot("A"), null, 0);

        List<Course.VisitStop> tooMany = IntStream.rangeClosed(1, CoursePlaces.MAX_ITEMS + 1)
                .mapToObj(i -> stop("S" + i, 35.1540, 129.1190, null))
                .toList();
        List<Course.VisitStop> duplicated = List.of(
                stop("B", 35.1540, 129.1190, null),
                stop("B", 35.1540, 129.1190, null));

        assertThatThrownBy(() -> course.replaceItems(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_EMPTY);
        assertThatThrownBy(() -> course.replaceItems(tooMany))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_LIMIT_EXCEEDED);
        assertThatThrownBy(() -> course.replaceItems(duplicated))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_DUPLICATED);

        assertThat(course.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("A"));
    }

    @Test
    @DisplayName("시작 지역을 해석할 수 없는 코스는 UNKNOWN_START_AREA로 거부하고 기존 아이템을 보존한다")
    void replaceItems_throws_whenStartAreaUnknown() {
        CourseIntent unknownArea = new CourseIntent("화성", false, null, null, List.of(), null, false);
        Course course = Course.restore(1L, 1L, UUID.randomUUID(), GenerationMode.AI, null,
                "제목", null, unknownArea, null, ShareInfo.initial(),
                List.of(CourseItem.restore(1L, 1L, (short) 1, PlaceRef.spot("A"), null, 0)),
                OffsetDateTime.now(), "AI_CHAT");

        assertThatThrownBy(() -> course.replaceItems(List.of(stop("B", 35.1540, 129.1190, null))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.UNKNOWN_START_AREA);
        assertThat(course.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("A"));
    }

    @Test
    @DisplayName("validateOwnedBy는 코스를 생성한 사용자가 아니면 COURSE_ACCESS_DENIED로 거부한다")
    void validateOwnedBy_throws_whenNotOwner() {
        Course course = aiCourse(); // userId = 1

        assertThatCode(() -> course.validateOwnedBy(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> course.validateOwnedBy(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ACCESS_DENIED);
        assertThatThrownBy(() -> course.validateOwnedBy(null))
                .isInstanceOf(BusinessException.class);
    }

    // ── edit (제목 · 소개 · 방문 스팟 일괄 편집) ──────────────────────────────

    /** 제목·소개·스팟 A가 담긴 편집 전 코스 */
    private static Course courseBeforeEdit() {
        Course course = aiCourse();
        course.applyAiContent("원래 제목", "원래 소개");
        course.addItem(PlaceRef.spot("A"), null, 0);
        return course;
    }

    private static void assertUnchanged(Course course) {
        assertThat(course.getTitle()).isEqualTo("원래 제목");
        assertThat(course.getDescription()).isEqualTo("원래 소개");
        assertThat(course.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("A"));
    }

    @Test
    @DisplayName("edit는 제목·소개의 앞뒤 공백을 제거해 반영하고 방문 스팟도 함께 교체한다")
    void edit_appliesStrippedContent_andReplacesItems() {
        Course course = courseBeforeEdit();

        course.edit("  광안리 바다 산책  ", "  바다를 따라 걷는 코스  ", List.of(stop("B", 35.1600, 129.1250, null)));

        assertThat(course.getTitle()).isEqualTo("광안리 바다 산책");
        assertThat(course.getDescription()).isEqualTo("바다를 따라 걷는 코스");
        assertThat(course.getItems()).extracting(CourseItem::getPlaceRef).containsExactly(PlaceRef.spot("B"));
    }

    @Test
    @DisplayName("edit에서 소개를 null이나 공백으로 보내면 소개가 지워진다")
    void edit_clearsDescription_whenNullOrBlank() {
        Course course = courseBeforeEdit();
        course.edit("제목", null, List.of(stop("A", 35.1540, 129.1190, null)));
        assertThat(course.getDescription()).isNull();

        Course other = courseBeforeEdit();
        other.edit("제목", "   ", List.of(stop("A", 35.1540, 129.1190, null)));
        assertThat(other.getDescription()).isNull();
    }

    @Test
    @DisplayName("edit는 제목 최대 길이와 소개 최대 길이까지는 허용한다")
    void edit_acceptsMaxLengths() {
        Course course = courseBeforeEdit();
        String maxTitle = "가".repeat(Course.MAX_TITLE_LENGTH);
        String maxDescription = "나".repeat(Course.MAX_DESCRIPTION_LENGTH);

        course.edit(maxTitle, maxDescription, List.of(stop("A", 35.1540, 129.1190, null)));

        assertThat(course.getTitle()).isEqualTo(maxTitle);
        assertThat(course.getDescription()).isEqualTo(maxDescription);
    }

    @Test
    @DisplayName("edit는 제목이 null·공백·최대 길이 초과면 COURSE_TITLE_INVALID로 거부하고 아무것도 바꾸지 않는다")
    void edit_rejectsInvalidTitle_andKeepsState() {
        Course course = courseBeforeEdit();
        List<Course.VisitStop> stops = List.of(stop("B", 35.1600, 129.1250, null));

        for (String invalidTitle : new String[]{null, "   ", "가".repeat(Course.MAX_TITLE_LENGTH + 1)}) {
            assertThatThrownBy(() -> course.edit(invalidTitle, "새 소개", stops))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_TITLE_INVALID);
        }
        assertUnchanged(course);
    }

    @Test
    @DisplayName("edit는 소개가 최대 길이를 넘으면 COURSE_DESCRIPTION_TOO_LONG으로 거부하고 아무것도 바꾸지 않는다")
    void edit_rejectsTooLongDescription_andKeepsState() {
        Course course = courseBeforeEdit();

        assertThatThrownBy(() -> course.edit("새 제목", "나".repeat(Course.MAX_DESCRIPTION_LENGTH + 1),
                List.of(stop("B", 35.1600, 129.1250, null))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_DESCRIPTION_TOO_LONG);
        assertUnchanged(course);
    }

    @Test
    @DisplayName("edit는 스팟 목록이 잘못되면 제목·소개도 반영하지 않는다 (원자적 편집)")
    void edit_keepsContent_whenItemsInvalid() {
        Course course = courseBeforeEdit();

        assertThatThrownBy(() -> course.edit("새 제목", "새 소개", List.of()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_EMPTY);
        assertUnchanged(course);
    }


    // ── 공유 (publish / unpublish / markViewed) ────────────────────────────

    @Test
    @DisplayName("publish는 토큰이 없을 때만 공급자를 호출해 발급하고, 공개 상태로 바꾼다")
    void publish_issuesTokenOnce() {
        Course course = aiCourse();
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();

        course.publish(() -> { calls.incrementAndGet(); return "token-1"; });

        assertThat(course.isPublic()).isTrue();
        assertThat(course.getShareInfo().shareToken()).isEqualTo("token-1");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 공개인 코스에 publish를 다시 호출하면 공급자를 부르지 않고 상태도 그대로다 (멱등)")
    void publish_isIdempotent_whenAlreadyPublic() {
        Course course = aiCourse();
        course.publish(() -> "token-1");

        course.publish(() -> { throw new AssertionError("공급자가 호출되면 안 된다"); });

        assertThat(course.isPublic()).isTrue();
        assertThat(course.getShareInfo().shareToken()).isEqualTo("token-1");
    }

    @Test
    @DisplayName("unpublish는 토큰·조회수·좋아요를 보존하고, 재공개 시 새 토큰을 무시해 같은 링크가 살아난다")
    void unpublish_keepsToken_andRepublishReusesIt() {
        Course course = aiCourse();
        course.publish(() -> "token-1");
        course.markViewed();
        course.markViewed();

        course.unpublish();
        assertThat(course.isPublic()).isFalse();
        assertThat(course.getShareInfo().shareToken()).isEqualTo("token-1");
        assertThat(course.getShareInfo().viewCount()).isEqualTo(2);

        course.publish(() -> "token-2");
        assertThat(course.isPublic()).isTrue();
        assertThat(course.getShareInfo().shareToken()).isEqualTo("token-1");
        assertThat(course.getShareInfo().viewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("markViewed는 메모리상 조회수만 1 올리고 공개 여부·토큰은 건드리지 않는다")
    void markViewed_incrementsViewCountOnly() {
        Course course = aiCourse();
        course.publish(() -> "token-1");

        course.markViewed();

        assertThat(course.getShareInfo().viewCount()).isEqualTo(1);
        assertThat(course.getShareInfo().likeCount()).isZero();
        assertThat(course.isPublic()).isTrue();
        assertThat(course.getShareInfo().shareToken()).isEqualTo("token-1");
    }

    @Test
    @DisplayName("shareInfo 없이는 코스를 복원할 수 없다 — 공유 상태는 항상 존재한다 (불변식)")
    void restore_throws_whenShareInfoNull() {
        assertThatThrownBy(() -> Course.restore(1L, 1L, UUID.randomUUID(), GenerationMode.AI, null,
                "제목", null, INTENT, null, null, List.of(), OffsetDateTime.now(), "AI_CHAT"))
                .isInstanceOf(NullPointerException.class);
    }
}
