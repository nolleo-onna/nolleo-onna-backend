package com.nolleo.onna.domain.review.infrastructure.persistence;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.review.domain.exception.ReviewErrorCode;
import com.nolleo.onna.domain.review.domain.model.Review;
import com.nolleo.onna.domain.review.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    @Override
    public Optional<Review> findByUserIdAndMapPlaceId(Long userId, Long mapPlaceId) {
        return jpaRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId)
                .map(ReviewEntity::toDomain);
    }

    @Override
    public Review updateRating(Long userId, Long mapPlaceId, int newRating) {
        ReviewEntity entity = jpaRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId)
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
        entity.updateRating(newRating);
        return jpaRepository.save(entity).toDomain();
    }
}