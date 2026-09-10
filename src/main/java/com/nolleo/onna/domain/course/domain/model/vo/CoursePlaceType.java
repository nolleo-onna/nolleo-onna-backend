package com.nolleo.onna.domain.course.domain.model.vo;

/**
 * 코스 아이템이 참조하는 장소의 원본 테이블 구분 (generated_course_items.place_type).
 *
 * Map 컨텍스트의 PlaceType과 값이 같지만 일부러 별도로 둔다 — Course 컨텍스트가 Map 도메인 모델을
 * import하면 ACL 경계가 깨진다. 값이 같은 것은 우연이지 의존 관계가 아니다.
 *
 *   SPOT — sp_spots.content_id (관광공사)
 *   FOOD — fd_food_places.id (착한가게)
 */
public enum CoursePlaceType {
    SPOT,
    FOOD
}
