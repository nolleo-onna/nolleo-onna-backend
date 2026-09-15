package com.nolleo.onna.domain.course.domain.model.vo;

/**
 * 예산 등급 — 폼의 5단계 선택값. 등급마다 식사·카페 슬롯 수와 식사·카페 1곳당 가격 상한을 정한다.
 *
 * 상한만 두면 1만원을 세 곳으로 나눠 3,333원이 되어 필터가 늘 풀리므로, 낮은 예산은 슬롯 자체를 줄인다.
 * 무지출은 식사·카페를 아예 빼고 관광지만 담는다(관광지는 가격 데이터가 없어 무료로 간주).
 *
 *   amount     intent.budget에 저장하는 대표 금액(원). 제한없음은 null — 기존 JSONB 구조(Integer budget)를 그대로 유지한다
 *   perItemCap 식사·카페 1곳당 가격 상한(원). 없으면 가격 필터를 걸지 않는다
 *
 * 챗봇이 뽑은 자유 금액은 fromAmount로 가장 가까운 등급으로 내림한다 (25,000원 → UNDER_10K).
 */
public enum BudgetTier {

    /** 무지출 — 식사·카페 제외, 관광지 4곳 */
    NONE(0, null, new SlotHints(0, 0, 4, 0)),
    /** 1만원 — 식사 1끼 */
    UNDER_10K(10_000, 10_000, new SlotHints(1, 0, 3, 0)),
    /** 3만원 — 식사 2끼 + 카페 */
    UNDER_30K(30_000, 12_000, new SlotHints(2, 1, 3, 0)),
    /** 5만원 — 식사 2끼 + 카페 */
    UNDER_50K(50_000, 20_000, new SlotHints(2, 1, 3, 0)),
    /** 제한없음 — 기본 패턴, 가격 필터 없음 */
    UNLIMITED(null, null, new SlotHints(2, 1, 3, 0));

    private final Integer amount;
    private final Integer perItemCap;
    private final SlotHints defaultSlots;

    BudgetTier(Integer amount, Integer perItemCap, SlotHints defaultSlots) {
        this.amount = amount;
        this.perItemCap = perItemCap;
        this.defaultSlots = defaultSlots;
    }

    /** intent.budget에 저장할 대표 금액 — 제한없음은 null */
    public Integer amount() {
        return amount;
    }

    /** 식사·카페 1곳당 가격 상한 — 없으면 null (필터 없음) */
    public Integer perItemCap() {
        return perItemCap;
    }

    public boolean hasPriceCap() {
        return perItemCap != null;
    }

    /** 사용자가 슬롯을 따로 말하지 않았을 때 쓰는 기본 슬롯 */
    public SlotHints defaultSlots() {
        return defaultSlots;
    }

    /** 자유 금액을 등급으로 내림 — null은 제한없음, 0~9,999는 무지출, 10,000~29,999는 1만원 … */
    public static BudgetTier fromAmount(Integer amount) {
        if (amount == null) return UNLIMITED;
        if (amount >= UNDER_50K.amount) return UNDER_50K;
        if (amount >= UNDER_30K.amount) return UNDER_30K;
        if (amount >= UNDER_10K.amount) return UNDER_10K;
        return NONE;
    }
}
