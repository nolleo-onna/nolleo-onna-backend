package com.nolleo.onna.domain.course.infrastructure.conversation;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.port.CourseGenerationLimiter;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * CourseGenerationLimiter의 Redis 구현.
 * key: course:ai:daily:{userId}:{yyyyMMdd} (KST 기준 날짜) — INCR로 원자적 카운트, TTL 1일.
 *
 * 장애 정책: Redis에 접근할 수 없으면 생성을 허용하지 않는다(fail-closed).
 * 한도를 셀 수 없는 상태에서 생성을 열어두면 유료 AI 호출이 무제한이 되므로,
 * 명확한 에러코드(GENERATION_LIMIT_UNAVAILABLE, 503)로 거절해 사용자가 잠시 후 재시도하게 한다.
 */
@Slf4j
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
        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofDays(1));
            }
        } catch (DataAccessException e) {
            log.error("일일 생성 한도 확인 실패 — 생성을 거절한다 userId={}", userId, e);
            throw new BusinessException(CourseErrorCode.GENERATION_LIMIT_UNAVAILABLE);
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
