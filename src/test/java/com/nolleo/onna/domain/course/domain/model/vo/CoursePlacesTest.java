package com.nolleo.onna.domain.course.domain.model.vo;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoursePlacesTest {

    private static List<PlaceRef> spots(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> PlaceRef.spot("S" + i)).toList();
    }

    @Test
    @DisplayName("서로 다른 장소 목록은 전달된 순서를 보존한 채 담긴다")
    void keepsOrder_whenValid() {
        CoursePlaces places = new CoursePlaces(List.of(PlaceRef.spot("C"), PlaceRef.spot("A"), PlaceRef.spot("B")));

        assertThat(places.refs()).containsExactly(PlaceRef.spot("C"), PlaceRef.spot("A"), PlaceRef.spot("B"));
        assertThat(places.allSpots()).isTrue();
    }

    @Test
    @DisplayName("null이거나 빈 목록이면 COURSE_ITEM_EMPTY")
    void throws_whenNullOrEmpty() {
        assertThatThrownBy(() -> new CoursePlaces(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_EMPTY);
        assertThatThrownBy(() -> new CoursePlaces(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_EMPTY);
    }

    @Test
    @DisplayName("MAX_ITEMS개까지는 허용하고, 하나라도 넘으면 COURSE_ITEM_LIMIT_EXCEEDED")
    void enforcesUpperBound() {
        assertThat(new CoursePlaces(spots(CoursePlaces.MAX_ITEMS)).refs()).hasSize(CoursePlaces.MAX_ITEMS);

        assertThatThrownBy(() -> new CoursePlaces(spots(CoursePlaces.MAX_ITEMS + 1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("같은 타입과 원본 ID가 두 번이면 COURSE_ITEM_DUPLICATED — 타입이 다르면 같은 원본 ID도 다른 장소다")
    void rejectsDuplicates_byTypeAndOriginalId() {
        assertThatThrownBy(() -> new CoursePlaces(List.of(PlaceRef.spot("A"), PlaceRef.spot("A"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ITEM_DUPLICATED);

        CoursePlaces mixed = new CoursePlaces(List.of(PlaceRef.spot("123"), new PlaceRef(CoursePlaceType.FOOD, "123")));
        assertThat(mixed.refs()).hasSize(2);
        assertThat(mixed.allSpots()).isFalse();
    }

    @Test
    @DisplayName("생성 후 원본 리스트를 바꿔도 VO는 영향받지 않는다 (방어적 복사)")
    void defensivelyCopies() {
        List<PlaceRef> source = new ArrayList<>(List.of(PlaceRef.spot("A")));
        CoursePlaces places = new CoursePlaces(source);

        source.add(PlaceRef.spot("B"));

        assertThat(places.refs()).containsExactly(PlaceRef.spot("A"));
    }
}
