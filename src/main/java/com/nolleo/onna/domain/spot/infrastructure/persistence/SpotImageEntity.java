package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.SpotImage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** [JPA 엔티티] sp_spot_images 테이블 매핑. */
@Entity
@Table(
        name = "sp_spot_images",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_spot_images_content_serial",
                columnNames = {"content_id", "serial_num"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "spot_images_seq")
    @SequenceGenerator(name = "spot_images_seq", sequenceName = "spot_images_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "content_id", length = 20, nullable = false)
    private String contentId;

    @Column(name = "origin_img_url", nullable = false)
    private String originImgUrl;

    @Column(name = "small_img_url")
    private String smallImgUrl;

    @Column(name = "img_name", length = 200)
    private String imgName;

    @Column(name = "cpyrht_div_cd", length = 10)
    private String cpyrhtDivCd;

    @Column(name = "serial_num", length = 20, nullable = false)
    private String serialNum;

    /** 엔티티 → 도메인 모델 변환 */
    public SpotImage toDomain() {
        return new SpotImage(id, contentId, originImgUrl, smallImgUrl, imgName, cpyrhtDivCd, serialNum);
    }
}