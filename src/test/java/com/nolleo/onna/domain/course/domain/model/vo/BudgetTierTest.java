package com.nolleo.onna.domain.course.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetTierTest {

    @Test
    @DisplayName("자유 금액은 가장 가까운 등급으로 내림한다 — null은 제한없음, 1만원 미만은 무지출")
    void fromAmount_floorsToTier() {
        assertThat(BudgetTier.fromAmount(null)).isEqualTo(BudgetTier.UNLIMITED);
        assertThat(BudgetTier.fromAmount(0)).isEqualTo(BudgetTier.NONE);
        assertThat(BudgetTier.fromAmount(9_999)).isEqualTo(BudgetTier.NONE);
        assertThat(BudgetTier.fromAmount(10_000)).isEqualTo(BudgetTier.UNDER_10K);
        assertThat(BudgetTier.fromAmount(25_000)).isEqualTo(BudgetTier.UNDER_10K);
        assertThat(BudgetTier.fromAmount(30_000)).isEqualTo(BudgetTier.UNDER_30K);
        assertThat(BudgetTier.fromAmount(50_000)).isEqualTo(BudgetTier.UNDER_50K);
        assertThat(BudgetTier.fromAmount(1_000_000)).isEqualTo(BudgetTier.UNDER_50K);
    }

    @Test
    @DisplayName("등급별 대표 금액·1곳 상한·기본 슬롯 — 무지출과 제한없음은 가격 필터가 없다")
    void tierAttributes() {
        assertThat(BudgetTier.NONE.amount()).isZero();
        assertThat(BudgetTier.NONE.hasPriceCap()).isFalse();
        assertThat(BudgetTier.NONE.defaultSlots()).isEqualTo(new SlotHints(0, 0, 4, 0));

        assertThat(BudgetTier.UNDER_10K.perItemCap()).isEqualTo(10_000);
        assertThat(BudgetTier.UNDER_30K.perItemCap()).isEqualTo(12_000);
        assertThat(BudgetTier.UNDER_50K.perItemCap()).isEqualTo(20_000);

        assertThat(BudgetTier.UNLIMITED.amount()).isNull();
        assertThat(BudgetTier.UNLIMITED.hasPriceCap()).isFalse();
        assertThat(BudgetTier.UNLIMITED.defaultSlots()).isEqualTo(new SlotHints(2, 1, 3, 0));
    }
}
