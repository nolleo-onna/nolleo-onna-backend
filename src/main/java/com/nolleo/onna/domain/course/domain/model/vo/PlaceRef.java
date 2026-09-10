package com.nolleo.onna.domain.course.domain.model.vo;

import java.util.Objects;

/**
 * 코스 아이템이 가리키는 장소 참조 (place_type + original_id).
 *
 * original_id 하나로는 sp_spots.content_id와 fd_food_places.id를 구분할 수 없으므로
 * 반드시 타입과 함께 다닌다. 값 객체라 equals/hashCode가 (type, originalId) 기준이며,
 * 스팟 일괄 조회 결과의 Map 키로 안전하게 쓸 수 있다.
 */
public record PlaceRef(CoursePlaceType type, String originalId) {

    public PlaceRef {
        Objects.requireNonNull(type, "type은 필수입니다.");
        if (originalId == null || originalId.isBlank()) {
            throw new IllegalArgumentException("originalId는 비어 있을 수 없습니다.");
        }
    }

    /** 관광공사 스팟 참조 — 코스 생성 파이프라인은 항상 이 팩토리를 쓴다 */
    public static PlaceRef spot(String contentId) {
        return new PlaceRef(CoursePlaceType.SPOT, contentId);
    }

    public boolean isSpot() {
        return type == CoursePlaceType.SPOT;
    }
}
