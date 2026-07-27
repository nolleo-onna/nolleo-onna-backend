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
        String key = KEY_PREFIX + userId + ":" + LocalDate.now(KST).format(DATE_FORMAT);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        return count != null && count <= DAILY_LIMIT;
    }
}
