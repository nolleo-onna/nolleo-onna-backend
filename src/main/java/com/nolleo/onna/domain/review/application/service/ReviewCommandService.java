package com.nolleo.onna.domain.review.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import com.nolleo.onna.domain.review.domain.exception.ReviewErrorCode;
import com.nolleo.onna.domain.review.domain.model.Review;
import com.nolleo.onna.domain.review.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final MapPlaceRepository mapPlaceRepository;
    private final RatingCacheService ratingCacheService;

    @Transactional
    public void createReview(Long userId, Long mapPlaceId, int rating) {
        mapPlaceRepository.findById(mapPlaceId)
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.MAP_PLACE_NOT_FOUND));

        if (reviewRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId).isPresent()) {
            throw new BusinessException(ReviewErrorCode.DUPLICATE_REVIEW);
        }

        reviewRepository.save(new Review(null, mapPlaceId, userId, rating, null));
        mapPlaceRepository.incrementRating(mapPlaceId, rating);
        ratingCacheService.updateCacheOnNewReview(mapPlaceId, rating);
    }

    @Transactional
    public void updateReview(Long userId, Long mapPlaceId, int newRating) {
        mapPlaceRepository.findById(mapPlaceId)
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.MAP_PLACE_NOT_FOUND));

        Review existing = reviewRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId)
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));

        int oldRating = existing.rating();
        reviewRepository.updateRating(userId, mapPlaceId, newRating);
        mapPlaceRepository.updateAvgRating(mapPlaceId, oldRating, newRating);
        ratingCacheService.updateCacheOnReviewUpdate(mapPlaceId, oldRating, newRating);
    }
}