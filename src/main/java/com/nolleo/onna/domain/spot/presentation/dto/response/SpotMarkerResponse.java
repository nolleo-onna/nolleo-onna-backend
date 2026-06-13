package com.nolleo.onna.domain.spot.presentation.dto.response;

import com.nolleo.onna.domain.spot.domain.entity.Spots;

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
    public static SpotMarkerResponse from(Spots spot) {
        return new SpotMarkerResponse(
                spot.getContentId(),
                spot.getContentTypeId(),
                spot.getTitle(),
                spot.getMapX(),
                spot.getMapY(),
                spot.getFirstImage(),
                spot.getLclsSystm1(),
                spot.getLclsSystm2(),
                spot.getLclsSystm3()
        );
    }
}
