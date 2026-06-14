package com.nolleo.onna.domain.spot.domain.model;

import java.time.OffsetDateTime;

/** [도메인 모델] 관광 장소 가격 요약 — 조회 전용 데이터 모델. */
public class SpotPriceSummary {

    private final String spotContentId;
    private final Integer minPrice;
    private final Integer avgPrice;
    private final String representativeMenuName;
    private final Integer representativePrice;
    private final int menuCount;
    private final int sourceCount;
    private final OffsetDateTime updatedAt;

    public SpotPriceSummary(String spotContentId, Integer minPrice, Integer avgPrice,
                            String representativeMenuName, Integer representativePrice,
                            int menuCount, int sourceCount, OffsetDateTime updatedAt) {
        this.spotContentId = spotContentId;
        this.minPrice = minPrice;
        this.avgPrice = avgPrice;
        this.representativeMenuName = representativeMenuName;
        this.representativePrice = representativePrice;
        this.menuCount = menuCount;
        this.sourceCount = sourceCount;
        this.updatedAt = updatedAt;
    }

    public String getSpotContentId()          { return spotContentId; }
    public Integer getMinPrice()              { return minPrice; }
    public Integer getAvgPrice()              { return avgPrice; }
    public String getRepresentativeMenuName() { return representativeMenuName; }
    public Integer getRepresentativePrice()   { return representativePrice; }
    public int getMenuCount()                 { return menuCount; }
    public int getSourceCount()               { return sourceCount; }
    public OffsetDateTime getUpdatedAt()      { return updatedAt; }
}