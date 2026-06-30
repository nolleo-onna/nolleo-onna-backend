package com.nolleo.onna.domain.review.infrastructure.persistence;

import com.nolleo.onna.domain.review.domain.model.Review;
import com.nolleo.onna.domain.review.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewJpaRepository jpaRepository;

    @Override
    public Review save(Review review) {
        return jpaRepository.save(ReviewEntity.from(review)).toDomain();
    }

    @Override
    public double calculateAvgRating(Long mapPlaceId) {
        return jpaRepository.calculateAvgRating(mapPlaceId);
    }

    @Override
    public long countByMapPlaceId(Long mapPlaceId) {
        return jpaRepository.countByMapPlaceId(mapPlaceId);
    }
}