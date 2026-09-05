package com.nolleo.onna.domain.review.domain.repository;

import com.nolleo.onna.domain.review.domain.model.Review;

import java.util.Optional;

public interface ReviewRepository {

    Review save(Review review);

    double calculateAvgRating(Long mapPlaceId);

    long countByMapPlaceId(Long mapPlaceId);

    Optional<Review> findByUserIdAndMapPlaceId(Long userId, Long mapPlaceId);

    Review updateRating(Long userId, Long mapPlaceId, int newRating);
}