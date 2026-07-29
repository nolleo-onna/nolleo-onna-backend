package com.nolleo.onna.domain.favorite.domain.model;

import java.time.OffsetDateTime;

public record Favorite(
        Long id,
        Long userId,
        Long mapPlaceId,
        OffsetDateTime createdAt
) {}
