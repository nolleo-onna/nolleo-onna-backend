package com.nolleo.onna.domain.review.application.service;

import com.nolleo.onna.domain.review.domain.repository.ReviewRepository;
import com.nolleo.onna.domain.review.presentation.dto.response.RatingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingCacheServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ReviewRepository reviewRepository;
    @Mock HashOperations<String, Object, Object> hashOps;

    @InjectMocks RatingCacheService ratingCacheService;

    private static final Long MAP_PLACE_ID = 10L;
    private static final String REDIS_KEY   = "map_place:10:rating";

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForHash()).willReturn(hashOps);
    }

    @Test
    @DisplayName("Redis 캐시가 있으면 Redis 값을 그대로 반환한다")
    void getRating_returnsCachedValue_whenCacheHit() {
        // given
        given(hashOps.get(REDIS_KEY, "avg")).willReturn("4.5");
        given(hashOps.get(REDIS_KEY, "count")).willReturn("200");

        // when
        RatingResponse result = ratingCacheService.getRating(MAP_PLACE_ID);

        // then
        assertThat(result.avgRating()).isEqualTo(4.5);
        assertThat(result.reviewCount()).isEqualTo(200);
        verifyNoInteractions(reviewRepository);
    }

    @Test
    @DisplayName("Redis 캐시 미스 시 DB에서 계산하고 Redis에 저장한다")
    void getRating_fallsBackToDb_whenCacheMiss() {
        // given
        given(hashOps.get(REDIS_KEY, "avg")).willReturn(null);
        given(hashOps.get(REDIS_KEY, "count")).willReturn(null);
        given(reviewRepository.calculateAvgRating(MAP_PLACE_ID)).willReturn(4.2);
        given(reviewRepository.countByMapPlaceId(MAP_PLACE_ID)).willReturn(50L);

        // when
        RatingResponse result = ratingCacheService.getRating(MAP_PLACE_ID);

        // then
        assertThat(result.avgRating()).isEqualTo(4.2);
        assertThat(result.reviewCount()).isEqualTo(50);
        verify(hashOps).put(REDIS_KEY, "avg", "4.2");
        verify(hashOps).put(REDIS_KEY, "count", "50");
    }

    @Test
    @DisplayName("리뷰가 없는 장소는 avgRating 0.0, reviewCount 0을 반환한다")
    void getRating_returnsZero_whenNoReviews() {
        // given
        given(hashOps.get(REDIS_KEY, "avg")).willReturn(null);
        given(hashOps.get(REDIS_KEY, "count")).willReturn(null);
        given(reviewRepository.calculateAvgRating(MAP_PLACE_ID)).willReturn(0.0);
        given(reviewRepository.countByMapPlaceId(MAP_PLACE_ID)).willReturn(0L);

        // when
        RatingResponse result = ratingCacheService.getRating(MAP_PLACE_ID);

        // then
        assertThat(result.avgRating()).isEqualTo(0.0);
        assertThat(result.reviewCount()).isZero();
    }

    @Test
    @DisplayName("Redis 장애 시 DB fallback으로 평점을 반환한다")
    void getRating_fallsBackToDb_whenRedisThrows() {
        // given
        given(hashOps.get(anyString(), anyString())).willThrow(new RuntimeException("Redis 연결 실패"));
        given(reviewRepository.calculateAvgRating(MAP_PLACE_ID)).willReturn(3.8);
        given(reviewRepository.countByMapPlaceId(MAP_PLACE_ID)).willReturn(100L);

        // when
        RatingResponse result = ratingCacheService.getRating(MAP_PLACE_ID);

        // then
        assertThat(result.avgRating()).isEqualTo(3.8);
        assertThat(result.reviewCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("캐시가 있으면 신규 리뷰 등록 시 증분 방식으로 평점을 소수 2자리로 반올림해 갱신한다")
    void updateCacheOnNewReview_updatesIncrementally_whenCacheExists() {
        // given — 기존: 평균 4.0, 리뷰 10개
        given(hashOps.get(REDIS_KEY, "avg")).willReturn("4.0");
        given(hashOps.get(REDIS_KEY, "count")).willReturn("10");

        // when — 새 리뷰 5점 등록
        ratingCacheService.updateCacheOnNewReview(MAP_PLACE_ID, 5);

        // then — (4.0 * 10 + 5) / 11 = 4.090909... → ROUND(2) = 4.09
        // DB avg_rating(NUMERIC(4,2))과 동일한 정밀도로 맞춰 두 엔드포인트 간 불일치를 방지한다
        verify(hashOps).put(eq(REDIS_KEY), eq("avg"), eq("4.09"));
        verify(hashOps).put(eq(REDIS_KEY), eq("count"), eq("11"));
    }

    @Test
    @DisplayName("DB fallback 시 평점을 소수 2자리로 반올림해 반환한다")
    void getRating_roundsToTwoDecimalPlaces_whenLoadedFromDb() {
        // given — AVG() 결과가 무한소수인 경우
        given(hashOps.get(REDIS_KEY, "avg")).willReturn(null);
        given(hashOps.get(REDIS_KEY, "count")).willReturn(null);
        given(reviewRepository.calculateAvgRating(MAP_PLACE_ID)).willReturn(4.090909090909091);
        given(reviewRepository.countByMapPlaceId(MAP_PLACE_ID)).willReturn(11L);

        // when
        RatingResponse result = ratingCacheService.getRating(MAP_PLACE_ID);

        // then — GET /map/places의 avg_rating(4.09)과 일치해야 한다
        assertThat(result.avgRating()).isEqualTo(4.09);
    }

    @Test
    @DisplayName("캐시 미스 시 신규 리뷰 등록은 DB에서 재계산 후 캐시를 갱신한다")
    void updateCacheOnNewReview_refreshesFromDb_whenCacheMiss() {
        // given
        given(hashOps.get(REDIS_KEY, "avg")).willReturn(null);
        given(hashOps.get(REDIS_KEY, "count")).willReturn(null);
        given(reviewRepository.calculateAvgRating(MAP_PLACE_ID)).willReturn(5.0);
        given(reviewRepository.countByMapPlaceId(MAP_PLACE_ID)).willReturn(1L);

        // when
        ratingCacheService.updateCacheOnNewReview(MAP_PLACE_ID, 5);

        // then
        verify(hashOps).put(REDIS_KEY, "avg", "5.0");
        verify(hashOps).put(REDIS_KEY, "count", "1");
    }
}