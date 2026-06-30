package com.nolleo.onna.domain.review.presentation.dto.response;

public record RatingResponse(
        double avgRating,
        long reviewCount
) {
    public static RatingResponse empty() {
        return new RatingResponse(0.0, 0);
    }
}