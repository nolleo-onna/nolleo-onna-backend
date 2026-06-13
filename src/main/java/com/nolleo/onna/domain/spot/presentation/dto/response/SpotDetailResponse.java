package com.nolleo.onna.domain.spot.presentation.dto.response;

import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.SpotDetail;
import com.nolleo.onna.domain.spot.domain.model.SpotImage;
import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;

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
        public static ImageInfo from(SpotImage image) {
            return new ImageInfo(image.getOriginImgUrl(), image.getSmallImgUrl(), image.getImgName());
        }
    }

    public static SpotDetailResponse of(
            Spot spot,
            SpotDetail detail,
            List<SpotImage> images,
            SpotPriceSummary priceSummary
    ) {
        GeoCoordinate geo = spot.getGeoCoordinate();
        return new SpotDetailResponse(
                spot.getContentId(),
                spot.getContentTypeId(),
                spot.getTitle(),
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                spot.getFirstImage(),
                spot.getFirstImage2(),
                spot.getLclsSystm1(),
                spot.getLclsSystm2(),
                spot.getLclsSystm3(),
                spot.getLDongRegnCd(),
                spot.getLDongSignguCd(),
                detail != null ? detail.getTel() : null,
                detail != null ? detail.getHomepage() : null,
                detail != null ? detail.getAddr1() : null,
                detail != null ? detail.getAddr2() : null,
                detail != null ? detail.getZipcode() : null,
                detail != null ? detail.getOverview() : null,
                detail != null ? detail.getParkingAvailable() : null,
                images.stream().map(ImageInfo::from).toList(),
                priceSummary != null ? priceSummary.getMinPrice() : null,
                priceSummary != null ? priceSummary.getAvgPrice() : null,
                priceSummary != null ? priceSummary.getRepresentativeMenuName() : null,
                priceSummary != null ? priceSummary.getRepresentativePrice() : null
        );
    }
}