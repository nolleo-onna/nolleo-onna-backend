package com.nolleo.onna.domain.course.infrastructure.conversation;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.port.CourseGenerationLimiter;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * CourseGenerationLimiter의 Redis 구현.
 * key: course:ai:daily:{userId}:{yyyyMMdd} (KST 기준 날짜) — INCR로 원자적 카운트, TTL 1일 (RedisDailyCounter).
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

    private final RedisDailyCounter dailyCounter;

    @Override
    public boolean tryConsume(Long userId) {
        try {
            return dailyCounter.increment(KEY_PREFIX, userId) <= DAILY_LIMIT;
        } catch (DataAccessException e) {
            log.error("일일 생성 한도 확인 실패 — 생성을 거절한다 userId={}", userId, e);
            throw new BusinessException(CourseErrorCode.GENERATION_LIMIT_UNAVAILABLE);
        }
    }

    @Override
    public void refund(Long userId) {
        dailyCounter.decrementAndCleanup(KEY_PREFIX, userId);
    }
}
