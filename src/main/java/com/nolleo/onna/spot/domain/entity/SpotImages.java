package com.nolleo.onna.spot.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sp_spot_images",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_spot_images_content_serial",
                columnNames = {"content_id", "serial_num"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotImages {

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

    public static SpotImages create(
            String contentId,
            String originImgUrl,
            String smallImgUrl,
            String imgName,
            String cpyrhtDivCd,
            String serialNum
    ) {
        SpotImages image = new SpotImages();
        image.contentId = contentId;
        image.originImgUrl = originImgUrl;
        image.smallImgUrl = smallImgUrl;
        image.imgName = imgName;
        image.cpyrhtDivCd = cpyrhtDivCd;
        image.serialNum = serialNum;
        return image;
    }
}