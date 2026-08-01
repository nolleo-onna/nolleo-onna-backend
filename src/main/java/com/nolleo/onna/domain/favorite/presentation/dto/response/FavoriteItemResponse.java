package com.nolleo.onna.domain.favorite.presentation.dto.response;

import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import com.nolleo.onna.domain.map.domain.model.MapPlace;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FavoriteItemResponse(
        Long mapPlaceId,
        String name,
        String district,
        String category,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime favoritedAt
) {
    public static FavoriteItemResponse of(Favorite favorite, MapPlace place) {
        return new FavoriteItemResponse(
                place.getId(),
                place.getName(),
                place.getDistrict(),
                place.getCategory() != null ? place.getCategory().name() : null,
                place.getLatitude(),
                place.getLongitude(),
                favorite.createdAt()
        );
    }
}
