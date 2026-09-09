package com.nolleo.onna.domain.course.domain.service;

import com.nolleo.onna.domain.course.domain.model.vo.SlotHints;
import com.nolleo.onna.domain.course.domain.model.vo.SlotPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlotPlannerTest {

    @Test
    @DisplayName("힌트가 없으면 기본 패턴(음식 2, 카페 1, 관광 3, 액티비티 0)을 적용한다")
    void plan_appliesDefaults_whenHintsNullOrEmpty() {
        SlotPlan fromNull = SlotPlanner.plan(null);
        SlotPlan fromEmpty = SlotPlanner.plan(new SlotHints(null, null, null, null));

        assertThat(fromNull).isEqualTo(new SlotPlan(2, 1, 3, 0));
        assertThat(fromEmpty).isEqualTo(new SlotPlan(2, 1, 3, 0));
        assertThat(fromNull.totalCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("명시적으로 0을 지정한 항목은 기본값으로 덮어쓰지 않고 그대로 존중한다")
    void plan_respectsExplicitZero() {
        SlotPlan plan = SlotPlanner.plan(new SlotHints(0, 0, null, null));

        assertThat(plan.foodCount()).isZero();
        assertThat(plan.cafeCount()).isZero();
        assertThat(plan.attractionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("언급한 항목만 덮어쓰고 나머지는 기본값을 유지한다")
    void plan_mergesPartialHints() {
        SlotPlan plan = SlotPlanner.plan(new SlotHints(null, null, null, 2));

        assertThat(plan).isEqualTo(new SlotPlan(2, 1, 3, 2));
    }
}
