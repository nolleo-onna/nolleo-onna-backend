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

        reviewRepository.save(new Review(null, mapPlaceId, userId, rating, null));
        ratingCacheService.updateCacheOnNewReview(mapPlaceId, rating);
    }
}