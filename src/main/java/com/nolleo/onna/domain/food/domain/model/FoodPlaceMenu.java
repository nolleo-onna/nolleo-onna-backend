package com.nolleo.onna.domain.food.domain.model;

import com.nolleo.onna.domain.food.domain.model.vo.Money;

import java.time.OffsetDateTime;

/** [도메인 모델] 음식 장소 메뉴 — 조회 전용 데이터 모델. */
public class FoodPlaceMenu {

    /** 메뉴 식별자 */
    private final Long id;

    /** 소속 음식 장소 ID */
    private final Long foodPlaceId;

    /** 메뉴/품목명 */
    private final String menuName;

    /** 가격 정보 (금액 + 통화 코드) */
    private final Money money;

    /** 원천 파일 내 품목 노출 순서 */
    private final Short displayOrder;

    /** 현재 가격의 수집 원천 */
    private final String source;

    /** 대표 메뉴 여부 */
    private final boolean representative;

    /** 가격 최종 관측 시각 */
    private final OffsetDateTime lastObservedAt;

    public FoodPlaceMenu(Long id, Long foodPlaceId, String menuName, Money money,
                         Short displayOrder, String source, boolean representative,
                         OffsetDateTime lastObservedAt) {
        this.id = id;
        this.foodPlaceId = foodPlaceId;
        this.menuName = menuName;
        this.money = money;
        this.displayOrder = displayOrder;
        this.source = source;
        this.representative = representative;
        this.lastObservedAt = lastObservedAt;
    }

    public Long getId() { return id; }
    public Long getFoodPlaceId() { return foodPlaceId; }
    public String getMenuName() { return menuName; }
    public Money getMoney() { return money; }
    public Short getDisplayOrder() { return displayOrder; }
    public String getSource() { return source; }
    public boolean isRepresentative() { return representative; }
    public OffsetDateTime getLastObservedAt() { return lastObservedAt; }
}
