package com.nolleo.onna.domain.course.domain.service;

import com.nolleo.onna.domain.course.domain.model.vo.BudgetTier;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
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

    @Test
    @DisplayName("총합이 코스 최대 개수를 넘으면 액티비티 → 관광 → 카페 → 식사 순으로 줄여 상한에 맞춘다")
    void plan_capsTotalAtMaxItems() {
        // 10 + 5 + 5 + 5 = 25 → 10 초과: 액티비티 5 전부, 관광 5 전부를 잘라 15
        SlotPlan plan = SlotPlanner.plan(new SlotHints(10, 5, 5, 5));

        assertThat(plan).isEqualTo(new SlotPlan(10, 5, 0, 0));
        assertThat(plan.totalCount()).isEqualTo(CoursePlaces.MAX_ITEMS);
    }

    @Test
    @DisplayName("식사만으로 상한을 넘으면 식사도 상한까지만 남긴다")
    void plan_capsFood_whenFoodAloneExceedsMax() {
        SlotPlan plan = SlotPlanner.plan(new SlotHints(40, 0, 0, 0));

        assertThat(plan).isEqualTo(new SlotPlan(CoursePlaces.MAX_ITEMS, 0, 0, 0));
    }

    @Test
    @DisplayName("음수 힌트는 0으로 취급한다 (기본값으로 되돌리지 않는다)")
    void plan_treatsNegativeAsZero() {
        SlotPlan plan = SlotPlanner.plan(new SlotHints(-3, null, -1, null));

        assertThat(plan).isEqualTo(new SlotPlan(0, 1, 0, 0));
    }

    @Test
    @DisplayName("예산 등급이 기본 슬롯을 정한다 — 무지출은 식사·카페 없이 관광 4, 1만원은 식사 1, 3만원 이상은 기본 패턴")
    void plan_usesBudgetTierDefaults() {
        assertThat(SlotPlanner.plan(null, BudgetTier.NONE)).isEqualTo(new SlotPlan(0, 0, 4, 0));
        assertThat(SlotPlanner.plan(null, BudgetTier.UNDER_10K)).isEqualTo(new SlotPlan(1, 0, 3, 0));
        assertThat(SlotPlanner.plan(null, BudgetTier.UNDER_30K)).isEqualTo(new SlotPlan(2, 1, 3, 0));
        assertThat(SlotPlanner.plan(null, BudgetTier.UNLIMITED)).isEqualTo(SlotPlanner.plan(null));
    }

    @Test
    @DisplayName("사용자가 명시한 힌트는 예산 등급 기본값보다 우선한다")
    void plan_explicitHintsOverrideTierDefaults() {
        SlotPlan plan = SlotPlanner.plan(new SlotHints(2, null, 1, null), BudgetTier.UNDER_10K);

        assertThat(plan).isEqualTo(new SlotPlan(2, 0, 1, 0)); // 식사·관광은 힌트, 카페·액티비티는 등급 기본값
    }
}
