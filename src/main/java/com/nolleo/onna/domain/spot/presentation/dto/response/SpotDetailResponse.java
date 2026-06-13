package com.nolleo.onna.domain.spot.presentation.dto.response;

import com.nolleo.onna.domain.spot.domain.entity.SpotDetails;
import com.nolleo.onna.domain.spot.domain.entity.SpotImages;
import com.nolleo.onna.domain.spot.domain.entity.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.entity.Spots;

import java.math.BigDecimal;
import java.util.List;

public record SpotDetailResponse(
        String contentId,
        String contentTypeId,
        String title,
        BigDecimal mapX,
        BigDecimal mapY,
        String firstImage,
        String firstImage2,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3,
        String lDongRegnCd,
        String lDongSignguCd,
        String tel,
        String homepage,
        String addr1,
        String addr2,
        String zipcode,
        String overview,
        Boolean parkingAvailable,
        List<ImageInfo> images,
        Integer minPrice,
        Integer avgPrice,
        String representativeMenuName,
        Integer representativePrice
) {
    public record ImageInfo(String originImgUrl, String smallImgUrl, String imgName) {
        public static ImageInfo from(SpotImages image) {
            return new ImageInfo(image.getOriginImgUrl(), image.getSmallImgUrl(), image.getImgName());
        }
    }

    public static SpotDetailResponse of(
            Spots spot,
            SpotDetails details,
            List<SpotImages> images,
            SpotPriceSummary priceSummary
    ) {
        return new SpotDetailResponse(
                spot.getContentId(),
                spot.getContentTypeId(),
                spot.getTitle(),
                spot.getMapX(),
                spot.getMapY(),
                spot.getFirstImage(),
                spot.getFirstImage2(),
                spot.getLclsSystm1(),
                spot.getLclsSystm2(),
                spot.getLclsSystm3(),
                spot.getLDongRegnCd(),
                spot.getLDongSignguCd(),
                details != null ? details.getTel() : null,
                details != null ? details.getHomepage() : null,
                details != null ? details.getAddr1() : null,
                details != null ? details.getAddr2() : null,
                details != null ? details.getZipcode() : null,
                details != null ? details.getOverview() : null,
                details != null ? details.getParkingAvailable() : null,
                images.stream().map(ImageInfo::from).toList(),
                priceSummary != null ? priceSummary.getMinPrice() : null,
                priceSummary != null ? priceSummary.getAvgPrice() : null,
                priceSummary != null ? priceSummary.getRepresentativeMenuName() : null,
                priceSummary != null ? priceSummary.getRepresentativePrice() : null
        );
    }
}
