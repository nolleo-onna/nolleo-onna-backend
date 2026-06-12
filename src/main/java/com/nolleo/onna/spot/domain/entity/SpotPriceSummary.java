package com.nolleo.onna.spot.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sp_spot_price_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotPriceSummary {

    @Id
    @Column(name = "spot_content_id", length = 20)
    private String spotContentId;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "avg_price")
    private Integer avgPrice;

    @Column(name = "representative_menu_name", length = 200)
    private String representativeMenuName;

    @Column(name = "representative_price")
    private Integer representativePrice;

    @Column(name = "menu_count", nullable = false)
    private int menuCount;

    @Column(name = "source_count", nullable = false)
    private int sourceCount;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static SpotPriceSummary create(String spotContentId) {
        SpotPriceSummary summary = new SpotPriceSummary();
        summary.spotContentId = spotContentId;
        summary.menuCount = 0;
        summary.sourceCount = 0;
        summary.updatedAt = OffsetDateTime.now();
        return summary;
    }

    public void update(
            Integer minPrice,
            Integer avgPrice,
            String representativeMenuName,
            Integer representativePrice,
            int menuCount,
            int sourceCount
    ) {
        this.minPrice = minPrice;
        this.avgPrice = avgPrice;
        this.representativeMenuName = representativeMenuName;
        this.representativePrice = representativePrice;
        this.menuCount = menuCount;
        this.sourceCount = sourceCount;
        this.updatedAt = OffsetDateTime.now();
    }
}