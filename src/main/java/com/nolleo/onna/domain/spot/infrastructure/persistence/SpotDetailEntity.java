package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.SpotDetail;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** [JPA 엔티티] sp_spot_details 테이블 매핑. */
@Entity
@Table(name = "sp_spot_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotDetailEntity {

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

    /** 엔티티 → 도메인 모델 변환 */
    public SpotDetail toDomain() {
        return new SpotDetail(
                contentId, tel, telName, homepage,
                addr1, addr2, zipcode,
                overview, overviewHash, intro,
                parkingAvailable, sourceCreatedAt, updatedAt
        );
    }
}