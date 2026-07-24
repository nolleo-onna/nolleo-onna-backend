package com.nolleo.onna.domain.course.domain.model.vo;

/**
 * SlotHints를 실제 카테고리별 방문 개수로 확정한 결과.
 * SlotPlanner가 SlotHints(사용자 명시) + 기본 패턴을 조합해 생성한다.
 */
public record SlotPlan(int foodCount, int cafeCount, int attractionCount, int activityCount) {

    public int totalCount() {
        return foodCount + cafeCount + attractionCount + activityCount;
    }
}
