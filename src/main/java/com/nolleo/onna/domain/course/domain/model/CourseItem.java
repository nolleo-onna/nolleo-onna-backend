package com.nolleo.onna.domain.course.domain.model;

import lombok.Getter;

/**
 * 코스에 포함된 방문 장소 엔티티 (Course의 자식 엔티티).
 * 외부에서 직접 변경할 수 없으며, 반드시 Course(Aggregate Root)를 통해서만 생성된다.
 */
@Getter
public class CourseItem {

    /** 내부 코스 아이템 식별자 (PK) */
    private Long id;

    /** 소속 코스 ID (FK) */
    private Long courseId;

    /** 코스 내 방문 순서 (1부터 시작) */
    private final Short serialNum;

    /** sp_spots.content_id 참조 */
    private final String spotContentId;

    /** 예상 방문 비용 (원) — 음식점만, 관광지는 null */
    private final Integer expectedCost;

    /** 이전 장소로부터 직선 거리 (미터) — 첫 장소는 지역 중심 기준 */
    private final Short distanceFromPrevM;

    /** 신규 생성용 생성자 — Course.addItem()에서만 호출 */
    CourseItem(Short serialNum, String spotContentId, Integer expectedCost, Short distanceFromPrevM) {
        this.id = null;
        this.courseId = null;
        this.serialNum = serialNum;
        this.spotContentId = spotContentId;
        this.expectedCost = expectedCost;
        this.distanceFromPrevM = distanceFromPrevM;
    }

    /** DB 조회값으로 도메인 객체 재구성 — Repository 구현체 전용 */
    public static CourseItem restore(Long id, Long courseId, Short serialNum,
                                               String spotContentId, Integer expectedCost,
                                               Short distanceFromPrevM) {
        CourseItem item = new CourseItem(serialNum, spotContentId, expectedCost, distanceFromPrevM);
        item.id = id;
        item.courseId = courseId;
        return item;
    }
}
