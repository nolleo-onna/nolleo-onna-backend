package com.nolleo.onna.domain.course.infrastructure.conversation;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.port.ChatMessageLimiter;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * ChatMessageLimiter의 Redis 구현.
 * key: course:chat:daily:{userId}:{yyyyMMdd} (KST 기준 날짜) — INCR로 원자적 카운트, TTL 1일 (RedisDailyCounter).
 *
 * 장애 정책: 생성 한도와 같은 fail-closed. 메시지마다 Gemini 파싱 비용이 나가므로
 * 셀 수 없는 상태에서는 CHAT_LIMIT_UNAVAILABLE(503)로 거절한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMessageLimiter implements ChatMessageLimiter {

    private static final String KEY_PREFIX = "course:chat:daily:";

    private final RedisDailyCounter dailyCounter;

    @Override
    public boolean tryConsume(Long userId, int dailyLimit) {
        try {
            return dailyCounter.increment(KEY_PREFIX, userId) <= dailyLimit;
        } catch (DataAccessException e) {
            log.error("일일 메시지 한도 확인 실패 — 메시지를 거절한다 userId={}", userId, e);
            throw new BusinessException(CourseErrorCode.CHAT_LIMIT_UNAVAILABLE);
        }
    }
}
