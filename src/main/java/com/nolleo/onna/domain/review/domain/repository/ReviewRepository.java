package com.nolleo.onna.domain.review.domain.repository;

import com.nolleo.onna.domain.review.domain.model.Review;

public interface ReviewRepository {

    Review save(Review review);

    double calculateAvgRating(Long mapPlaceId);

    long countByMapPlaceId(Long mapPlaceId);
}