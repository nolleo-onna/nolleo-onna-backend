package com.nolleo.onna.domain.review.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import com.nolleo.onna.domain.review.domain.exception.ReviewErrorCode;
import com.nolleo.onna.domain.review.domain.model.Review;
import com.nolleo.onna.domain.review.domain.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock MapPlaceRepository mapPlaceRepository;
    @Mock RatingCacheService ratingCacheService;

    @InjectMocks ReviewCommandService reviewCommandService;

    @Test
    @DisplayName("유효한 mapPlaceId와 rating으로 리뷰를 등록한다")
    void createReview_success() {
        // given
        Long userId = 1L;
        Long mapPlaceId = 10L;
        int rating = 4;

        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.of(mock(MapPlace.class)));
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        reviewCommandService.createReview(userId, mapPlaceId, rating);

        // then
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(mapPlaceRepository, times(1)).incrementRating(mapPlaceId, rating);
        verify(ratingCacheService, times(1)).updateCacheOnNewReview(mapPlaceId, rating);
    }

    @Test
    @DisplayName("존재하지 않는 mapPlaceId로 요청 시 MAP_PLACE_NOT_FOUND 예외를 던진다")
    void createReview_throwsException_whenMapPlaceNotFound() {
        // given
        Long mapPlaceId = 999L;
        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewCommandService.createReview(1L, mapPlaceId, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ReviewErrorCode.MAP_PLACE_NOT_FOUND));

        verify(reviewRepository, never()).save(any());
        verify(mapPlaceRepository, never()).incrementRating(any(), anyInt());
        verify(ratingCacheService, never()).updateCacheOnNewReview(any(), anyInt());
    }

    @Test
    @DisplayName("리뷰 저장 후 반드시 캐시 갱신을 호출한다")
    void createReview_updatesCache_afterSave() {
        // given
        Long mapPlaceId = 10L;
        int rating = 5;

        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.of(mock(MapPlace.class)));
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        reviewCommandService.createReview(1L, mapPlaceId, rating);

        // then — 저장 → DB 증분 → 캐시 갱신 순서 확인
        var inOrder = inOrder(reviewRepository, mapPlaceRepository, ratingCacheService);
        inOrder.verify(reviewRepository).save(any(Review.class));
        inOrder.verify(mapPlaceRepository).incrementRating(mapPlaceId, rating);
        inOrder.verify(ratingCacheService).updateCacheOnNewReview(mapPlaceId, rating);
    }
}