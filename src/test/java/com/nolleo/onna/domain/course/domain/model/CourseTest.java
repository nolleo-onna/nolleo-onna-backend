package com.nolleo.onna.domain.course.domain.model;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CourseType;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
}
