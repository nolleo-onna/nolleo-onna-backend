package com.nolleo.onna.domain.course.infrastructure.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 회원별 일일 카운터 — {prefix}{userId}:{yyyyMMdd} (KST 기준 날짜) 키에 INCR/DECR, 첫 증가 때 TTL 1일.
 * 코스 생성 횟수·챗봇 메시지 수 제한이 같은 패턴을 쓰므로 한 곳에 둔다.
 * Redis 예외(DataAccessException)는 그대로 올린다 — 장애 정책은 호출자(각 Limiter)가 정한다.
 */
@Component
@RequiredArgsConstructor
class RedisDailyCounter {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    /** 오늘 키를 1 올리고 증가 후 값을 돌려준다. 오늘 처음 만들어진 키면 TTL을 건다. */
    long increment(String prefix, Long userId) {
        String key = dailyKey(prefix, userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, TTL);
        }
        return count != null ? count : Long.MAX_VALUE; // null은 트랜잭션/파이프라인 모드에서만 나온다 — 한도 초과로 취급
    }

    /**
     * 오늘 키를 1 내린다. 0 이하가 되면 키를 지운다.
     * 증가와 감소 사이에 TTL이 만료된 경우 DECR이 TTL 없는 -1 키를 새로 만들기 때문에,
     * 그대로 두면 영구히 남아 다음날 한도를 잘못 늘려준다.
     */
    void decrementAndCleanup(String prefix, Long userId) {
        String key = dailyKey(prefix, userId);
        Long count = redisTemplate.opsForValue().decrement(key);
        if (count != null && count <= 0L) {
            redisTemplate.delete(key);
        }
    }

    private static String dailyKey(String prefix, Long userId) {
        return prefix + userId + ":" + LocalDate.now(KST).format(DATE_FORMAT);
    }
}
