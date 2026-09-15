package com.nolleo.onna.domain.course.domain.service;

import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.SlotHints;
import com.nolleo.onna.domain.course.domain.model.vo.SlotPlan;

/**
 * SlotHints(사용자 명시 힌트)를 실제 카테고리별 방문 개수(SlotPlan)로 확정한다.
 * 언급하지 않은 항목(null)은 기본 패턴 값을 적용하고, 명시적으로 0으로 지정한 항목은 그대로 존중한다.
 *
 * 힌트는 자연어에서 뽑은 값이라 신뢰할 수 없다 — 음수는 0으로, 총합은 한 코스에 담을 수 있는
 * 최대 개수(CoursePlaces.MAX_ITEMS)로 잘라 생성 경로도 편집 경로와 같은 불변식을 지킨다.
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
        return capTotal(
                nonNegative(hints.foodCount(), DEFAULT_FOOD_COUNT),
                nonNegative(hints.cafeCount(), DEFAULT_CAFE_COUNT),
                nonNegative(hints.attractionCount(), DEFAULT_ATTRACTION_COUNT),
                nonNegative(hints.activityCount(), DEFAULT_ACTIVITY_COUNT),
                CoursePlaces.MAX_ITEMS
        );
    }

    private static int nonNegative(Integer hint, int defaultValue) {
        if (hint == null) return defaultValue;
        return Math.max(0, hint);
    }

    /**
     * 총합이 max를 넘으면 액티비티 → 관광 → 카페 → 식사 순으로 줄인다.
     * 식사는 코스의 뼈대(동선·비용의 기준)라 가장 마지막까지 지킨다.
     */
    private static SlotPlan capTotal(int food, int cafe, int attraction, int activity, int max) {
        int over = food + cafe + attraction + activity - max;
        if (over <= 0) return new SlotPlan(food, cafe, attraction, activity);

        int cut = Math.min(over, activity);
        activity -= cut;
        over -= cut;

        cut = Math.min(over, attraction);
        attraction -= cut;
        over -= cut;

        cut = Math.min(over, cafe);
        cafe -= cut;
        over -= cut;

        food -= over;
        return new SlotPlan(food, cafe, attraction, activity);
    }
}
