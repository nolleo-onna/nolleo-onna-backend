package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** [JPA 엔티티] sp_spot_price_summary 테이블 매핑. */
@Entity
@Table(name = "sp_spot_price_summary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotPriceSummaryEntity {

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

    /** 엔티티 → 도메인 모델 변환 */
    public SpotPriceSummary toDomain() {
        return new SpotPriceSummary(
                spotContentId, minPrice, avgPrice,
                representativeMenuName, representativePrice,
                menuCount, sourceCount, updatedAt
        );
    }
}