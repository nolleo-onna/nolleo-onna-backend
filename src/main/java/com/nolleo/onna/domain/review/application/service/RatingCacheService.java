package com.nolleo.onna.domain.review.application.service;

import com.nolleo.onna.domain.review.domain.repository.ReviewRepository;
import com.nolleo.onna.domain.review.presentation.dto.response.RatingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "RatingCache")
public class RatingCacheService {

    private static final String KEY_PREFIX = "map_place:";
    private static final String KEY_SUFFIX = ":rating";
    private static final String FIELD_AVG   = "avg";
    private static final String FIELD_COUNT = "count";

    private final StringRedisTemplate redisTemplate;
    private final ReviewRepository reviewRepository;

    public RatingResponse getRating(Long mapPlaceId) {
        try {
            String key = buildKey(mapPlaceId);
            String avgStr   = (String) redisTemplate.opsForHash().get(key, FIELD_AVG);
            String countStr = (String) redisTemplate.opsForHash().get(key, FIELD_COUNT);

            if (avgStr != null && countStr != null) {
                return new RatingResponse(Double.parseDouble(avgStr), Long.parseLong(countStr));
            }

            return refreshCache(mapPlaceId, key);
        } catch (Exception e) {
            log.warn("Redis 조회 실패, DB fallback - mapPlaceId={}", mapPlaceId, e);
            return loadFromDb(mapPlaceId);
        }
    }

    /**
     * 리뷰 등록 시 Redis 캐시를 증분(incremental) 방식으로 갱신.
     * 캐시 미스 시 DB에서 전체 재계산 후 저장.
     */
    public void updateCacheOnNewReview(Long mapPlaceId, int newRating) {
        try {
            String key      = buildKey(mapPlaceId);
            String avgStr   = (String) redisTemplate.opsForHash().get(key, FIELD_AVG);
            String countStr = (String) redisTemplate.opsForHash().get(key, FIELD_COUNT);

            if (avgStr == null || countStr == null) {
                refreshCache(mapPlaceId, key);
                return;
            }

            long   oldCount = Long.parseLong(countStr);
            double oldAvg   = Double.parseDouble(avgStr);
            long   newCount = oldCount + 1;
            double newAvg   = Math.round((oldAvg * oldCount + newRating) / newCount * 100.0) / 100.0;

            redisTemplate.opsForHash().put(key, FIELD_AVG,   String.valueOf(newAvg));
            redisTemplate.opsForHash().put(key, FIELD_COUNT, String.valueOf(newCount));
        } catch (Exception e) {
            log.warn("Redis 캐시 갱신 실패 - mapPlaceId={}", mapPlaceId, e);
        }
    }

    private RatingResponse refreshCache(Long mapPlaceId, String key) {
        RatingResponse rating = loadFromDb(mapPlaceId);
        try {
            redisTemplate.opsForHash().put(key, FIELD_AVG,   String.valueOf(rating.avgRating()));
            redisTemplate.opsForHash().put(key, FIELD_COUNT, String.valueOf(rating.reviewCount()));
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패 - mapPlaceId={}", mapPlaceId, e);
        }
        return rating;
    }

    private RatingResponse loadFromDb(Long mapPlaceId) {
        double avg   = Math.round(reviewRepository.calculateAvgRating(mapPlaceId) * 100.0) / 100.0;
        long   count = reviewRepository.countByMapPlaceId(mapPlaceId);
        return new RatingResponse(avg, count);
    }

    /**
     * 리뷰 수정 시 Redis 캐시를 증분 방식으로 갱신.
     * avg = (old_avg * count - oldRating + newRating) / count
     * 캐시 미스 시 DB에서 전체 재계산.
     */
    public void updateCacheOnReviewUpdate(Long mapPlaceId, int oldRating, int newRating) {
        try {
            String key      = buildKey(mapPlaceId);
            String avgStr   = (String) redisTemplate.opsForHash().get(key, FIELD_AVG);
            String countStr = (String) redisTemplate.opsForHash().get(key, FIELD_COUNT);

            if (avgStr == null || countStr == null) {
                refreshCache(mapPlaceId, key);
                return;
            }

            long   count  = Long.parseLong(countStr);
            double oldAvg = Double.parseDouble(avgStr);
            double newAvg = count == 0 ? 0
                    : Math.round((oldAvg * count - oldRating + newRating) / count * 100.0) / 100.0;

            redisTemplate.opsForHash().put(key, FIELD_AVG, String.valueOf(newAvg));
        } catch (Exception e) {
            log.warn("Redis 캐시 갱신 실패(수정) - mapPlaceId={}", mapPlaceId, e);
        }
    }

    private String buildKey(Long mapPlaceId) {
        return KEY_PREFIX + mapPlaceId + KEY_SUFFIX;
    }
}