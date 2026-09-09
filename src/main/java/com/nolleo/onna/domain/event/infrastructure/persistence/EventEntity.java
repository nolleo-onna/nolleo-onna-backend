package com.nolleo.onna.domain.event.infrastructure.persistence;

import com.nolleo.onna.domain.event.domain.model.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** [JPA 엔티티] ev_events 테이블 매핑. */
@Entity
@Table(name = "ev_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventEntity {

    @Id
    @Column(name = "content_id", length = 20)
    private String contentId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "event_start_date")
    private LocalDate eventStartDate;

    @Column(name = "event_end_date")
    private LocalDate eventEndDate;

    @Column(name = "map_x", precision = 13, scale = 7)
    private BigDecimal mapX;

    @Column(name = "map_y", precision = 13, scale = 7)
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

    @Column(name = "tel")
    private String tel;

    @Column(name = "addr1", columnDefinition = "TEXT")
    private String addr1;

    @Column(name = "addr2", columnDefinition = "TEXT")
    private String addr2;

    @Column(name = "first_image", columnDefinition = "TEXT")
    private String firstImage;

    @Column(name = "first_image2", columnDefinition = "TEXT")
    private String firstImage2;

    @Column(name = "first_image_cpyrht_div_cd")
    private String firstImageCpyrhtDivCd;

    @Column(name = "event_place", columnDefinition = "TEXT")
    private String eventPlace;

    @Column(name = "play_time", columnDefinition = "TEXT")
    private String playTime;

    @Column(name = "use_time_festival", columnDefinition = "TEXT")
    private String useTimeFestival;

    @Column(name = "sponsor1")
    private String sponsor1;

    @Column(name = "sponsor1_tel")
    private String sponsor1Tel;

    @Column(name = "sponsor2")
    private String sponsor2;

    @Column(name = "sponsor2_tel")
    private String sponsor2Tel;

    @Column(name = "age_limit")
    private String ageLimit;

    @Column(name = "event_homepage", columnDefinition = "TEXT")
    private String eventHomepage;

    @Column(name = "source_modified_time")
    private OffsetDateTime sourceModifiedTime;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "inactive_since")
    private OffsetDateTime inactiveSince;

    /** 엔티티 → 도메인 모델 변환 */
    public Event toDomain() {
        return new Event(
                contentId, title, eventStartDate, eventEndDate,
                mapX, mapY,
                lDongRegnCd, lDongSignguCd,
                lclsSystm1, lclsSystm2, lclsSystm3,
                tel, addr1, addr2,
                firstImage, firstImage2, firstImageCpyrhtDivCd,
                eventPlace, playTime, useTimeFestival,
                sponsor1, sponsor1Tel, sponsor2, sponsor2Tel,
                ageLimit, eventHomepage,
                sourceModifiedTime, syncedAt, updatedAt,
                active, inactiveSince
        );
    }
}
