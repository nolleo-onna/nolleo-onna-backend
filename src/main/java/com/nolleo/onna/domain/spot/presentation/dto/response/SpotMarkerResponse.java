package com.nolleo.onna.domain.spot.presentation.dto.response;

import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;

import java.math.BigDecimal;

public record SpotMarkerResponse(
        String contentId,
        String contentTypeId,
        String title,
        BigDecimal mapX,
        BigDecimal mapY,
        String firstImage,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3
) {
    public static SpotMarkerResponse from(Spot spot) {
        GeoCoordinate geo = spot.getGeoCoordinate();
        return new SpotMarkerResponse(
                spot.getContentId(),
                spot.getContentTypeId(),
                spot.getTitle(),
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                spot.getFirstImage(),
                spot.getLclsSystm1(),
                spot.getLclsSystm2(),
                spot.getLclsSystm3()
        );
    }
}