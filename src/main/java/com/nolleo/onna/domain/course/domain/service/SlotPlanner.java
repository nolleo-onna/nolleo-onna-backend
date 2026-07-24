package com.nolleo.onna.domain.course.domain.service;

import com.nolleo.onna.domain.course.domain.model.vo.SlotHints;
import com.nolleo.onna.domain.course.domain.model.vo.SlotPlan;

/**
 * SlotHints(사용자 명시 힌트)를 실제 카테고리별 방문 개수(SlotPlan)로 확정한다.
 * 언급하지 않은 항목(null)은 기본 패턴 값을 적용하고, 명시적으로 0으로 지정한 항목은 그대로 존중한다.
 */
public class SlotPlanner {

    private static final int DEFAULT_FOOD_COUNT = 2;
    private static final int DEFAULT_CAFE_COUNT = 1;
    private static final int DEFAULT_ATTRACTION_COUNT = 3;
    private static final int DEFAULT_ACTIVITY_COUNT = 0;

    private SlotPlanner() {
    }

    public static SlotPlan plan(SlotHints hints) {
        if (hints == null || hints.isEmpty()) {
            return new SlotPlan(DEFAULT_FOOD_COUNT, DEFAULT_CAFE_COUNT, DEFAULT_ATTRACTION_COUNT, DEFAULT_ACTIVITY_COUNT);
        }
        return new SlotPlan(
                hints.foodCount() != null ? hints.foodCount() : DEFAULT_FOOD_COUNT,
                hints.cafeCount() != null ? hints.cafeCount() : DEFAULT_CAFE_COUNT,
                hints.attractionCount() != null ? hints.attractionCount() : DEFAULT_ATTRACTION_COUNT,
                hints.activityCount() != null ? hints.activityCount() : DEFAULT_ACTIVITY_COUNT
        );
    }
}
