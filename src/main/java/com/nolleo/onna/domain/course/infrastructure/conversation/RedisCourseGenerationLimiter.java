package com.nolleo.onna.domain.course.infrastructure.conversation;

import com.nolleo.onna.domain.course.application.port.CourseGenerationLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * CourseGenerationLimiter의 Redis 구현.
 * key: course:ai:daily:{userId}:{yyyyMMdd} (KST 기준 날짜) — INCR로 원자적 카운트, TTL 1일.
 */
@Component
@RequiredArgsConstructor
public class RedisCourseGenerationLimiter implements CourseGenerationLimiter {

    private static final String KEY_PREFIX = "course:ai:daily:";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryConsume(Long userId) {
        String key = dailyKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        return count != null && count <= DAILY_LIMIT;
    }

    @Override
    public void refund(Long userId) {
        String key = dailyKey(userId);
        Long count = redisTemplate.opsForValue().decrement(key);
        // 0 이하면 키를 정리한다.
        // 소비와 환불 사이에 TTL이 만료된 경우 DECR이 TTL 없는 -1 키를 새로 만들기 때문에,
        // 그대로 두면 영구히 남아 다음날 한도를 잘못 늘려준다.
        if (count != null && count <= 0L) {
            redisTemplate.delete(key);
        }
    }

    private String dailyKey(Long userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now(KST).format(DATE_FORMAT);
    }
}
