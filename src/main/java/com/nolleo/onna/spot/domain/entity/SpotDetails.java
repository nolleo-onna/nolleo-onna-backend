package com.nolleo.onna.spot.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sp_spot_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotDetails {

    @Id
    @Column(name = "content_id", length = 20)
    private String contentId;

    @Column(name = "tel", length = 50)
    private String tel;

    @Column(name = "tel_name", length = 100)
    private String telName;

    @Column(name = "homepage")
    private String homepage;

    @Column(name = "addr1")
    private String addr1;

    @Column(name = "addr2")
    private String addr2;

    @Column(name = "zipcode", length = 10)
    private String zipcode;

    @Column(name = "overview")
    private String overview;

    @Column(name = "overview_hash", length = 64)
    private String overviewHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "intro", columnDefinition = "jsonb")
    private String intro;

    @Column(name = "parking_available")
    private Boolean parkingAvailable;

    @Column(name = "source_created_at")
    private OffsetDateTime sourceCreatedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static SpotDetails create(String contentId) {
        SpotDetails details = new SpotDetails();
        details.contentId = contentId;
        details.updatedAt = OffsetDateTime.now();
        return details;
    }

    public void update(
            String tel,
            String telName,
            String homepage,
            String addr1,
            String addr2,
            String zipcode,
            String overview,
            String overviewHash,
            String intro,
            Boolean parkingAvailable,
            OffsetDateTime sourceCreatedAt
    ) {
        this.tel = tel;
        this.telName = telName;
        this.homepage = homepage;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.zipcode = zipcode;
        this.overview = overview;
        this.overviewHash = overviewHash;
        this.intro = intro;
        this.parkingAvailable = parkingAvailable;
        this.sourceCreatedAt = sourceCreatedAt;
        this.updatedAt = OffsetDateTime.now();
    }
}