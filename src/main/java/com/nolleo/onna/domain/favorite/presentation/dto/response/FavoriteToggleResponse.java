package com.nolleo.onna.domain.favorite.presentation.dto.response;

public record FavoriteToggleResponse(
        Long mapPlaceId,
        boolean favorited
) {}
