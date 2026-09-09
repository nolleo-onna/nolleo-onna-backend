package com.nolleo.onna.domain.course.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceRefTest {

    @Test
    @DisplayName("spot() 팩토리는 SPOT 타입 참조를 만든다")
    void spot_createsSpotRef() {
        PlaceRef ref = PlaceRef.spot("2760699");

        assertThat(ref.type()).isEqualTo(CoursePlaceType.SPOT);
        assertThat(ref.originalId()).isEqualTo("2760699");
        assertThat(ref.isSpot()).isTrue();
    }

    @Test
    @DisplayName("type이 null이면 생성할 수 없다")
    void constructor_throws_whenTypeNull() {
        assertThatThrownBy(() -> new PlaceRef(null, "1"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("originalId가 비어 있으면 생성할 수 없다")
    void constructor_throws_whenOriginalIdBlank() {
        assertThatThrownBy(() -> new PlaceRef(CoursePlaceType.SPOT, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlaceRef(CoursePlaceType.SPOT, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 originalId라도 타입이 다르면 다른 참조다 — Map 키로 충돌하지 않는다")
    void equality_distinguishesByType() {
        PlaceRef spot = new PlaceRef(CoursePlaceType.SPOT, "123");
        PlaceRef food = new PlaceRef(CoursePlaceType.FOOD, "123");

        Map<PlaceRef, String> map = new HashMap<>();
        map.put(spot, "스팟");
        map.put(food, "음식점");

        assertThat(spot).isNotEqualTo(food);
        assertThat(map).hasSize(2);
        assertThat(map.get(PlaceRef.spot("123"))).isEqualTo("스팟");
    }
}
