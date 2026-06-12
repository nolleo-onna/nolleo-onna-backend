package com.nolleo.onna.domain.spot.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sp_spots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spots {

    @Id
    @Column(name = "content_id", length = 20)
    private String contentId;

    @Column(name = "content_type_id", length = 8, nullable = false)
    private String contentTypeId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "source_tour_api", nullable = false)
    private boolean sourceTourApi;

    @Column(name = "source_busan_food", nullable = false)
    private boolean sourceBusanFood;

    @Column(name = "map_x", precision = 10, scale = 7)
    private BigDecimal mapX;

    @Column(name = "map_y", precision = 10, scale = 7)
    private BigDecimal mapY;

    @Column(name = "l_dong_regn_cd", length = 2)
    private String lDongRegnCd;

    @Column(name = "l_dong_signgu_cd", length = 5)
    private String lDongSignguCd;

    @Column(name = "lcls_systm_1", length = 10)
    private String lclsSystm1;

    @Column(name = "lcls_systm_2", length = 10)
    private String lclsSystm2;

    @Column(name = "lcls_systm_3", length = 10)
    private String lclsSystm3;

    @Column(name = "first_image")
    private String firstImage;

    @Column(name = "first_image2")
    private String firstImage2;

    @Column(name = "first_image_cpyrht_div_cd", length = 10)
    private String firstImageCpyrhtDivCd;

    @Column(name = "source_modified_time")
    private OffsetDateTime sourceModifiedTime;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "inactive_since")
    private OffsetDateTime inactiveSince;

}