package com.nolleo.onna.domain.review.domain.model;

import java.time.OffsetDateTime;

public record Review(
        Long id,
        Long mapPlaceId,
        Long userId,
        int rating,
        OffsetDateTime createdAt
) {}