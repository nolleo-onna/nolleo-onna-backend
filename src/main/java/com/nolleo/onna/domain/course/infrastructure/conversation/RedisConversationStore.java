package com.nolleo.onna.domain.course.infrastructure.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.course.application.dto.ConversationState;
import com.nolleo.onna.domain.course.application.port.ConversationStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * ConversationStore의 Redis 구현.
 * key: course:chat:{conversationId}  TTL 30분
 * 되묻기 후 사용자의 후속 입력을 기존 intent와 병합하고, 턴 수를 세기 위한 임시 저장소.
 *
 * 장애 정책: Redis 연결 실패·타임아웃은 요청을 실패시키지 않는다.
 *   save/delete → 로그만, find → empty(새 대화로 진행).
 *   take만은 예외를 그대로 올린다 — 생성 직전의 원자적 소유권 획득이라 실패를 숨기면 중복 생성으로 이어진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConversationStore implements ConversationStore {

    private static final String KEY_PREFIX = "course:chat:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String conversationId, ConversationState state) {
        try {
            String value = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationId, value, TTL);
        } catch (Exception e) {
            log.error("대화 상태 저장 실패 conversationId={}", conversationId, e);
        }
    }

    @Override
    public Optional<ConversationState> find(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return Optional.empty();
        try {
            return deserialize(conversationId, redisTemplate.opsForValue().get(KEY_PREFIX + conversationId));
        } catch (Exception e) {
            log.error("대화 상태 조회 실패 — 새 대화로 진행 conversationId={}", conversationId, e);
            return Optional.empty();
        }
    }

    /** GETDEL(Redis 6.2+)로 조회와 삭제를 한 명령에 처리한다 — 동시 요청 중 하나만 값을 받는다 */
    @Override
    public Optional<ConversationState> take(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return Optional.empty();
        return deserialize(conversationId, redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + conversationId));
    }

    @Override
    public void delete(String conversationId) {
        try {
            redisTemplate.delete(KEY_PREFIX + conversationId);
        } catch (Exception e) {
            log.error("대화 상태 삭제 실패 — TTL 만료로 정리된다 conversationId={}", conversationId, e);
        }
    }

    /** 저장된 JSON이 깨져 있으면(스키마 변경 등) 없는 것으로 취급한다 */
    private Optional<ConversationState> deserialize(String conversationId, String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, ConversationState.class));
        } catch (Exception e) {
            log.error("대화 상태 역직렬화 실패 conversationId={}", conversationId, e);
            return Optional.empty();
        }
    }
}
