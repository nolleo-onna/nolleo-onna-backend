package com.nolleo.onna.domain.favorite.presentation.dto.response;

public record FavoriteStatusResponse(
        Long mapPlaceId,
        boolean favorited
) {}
