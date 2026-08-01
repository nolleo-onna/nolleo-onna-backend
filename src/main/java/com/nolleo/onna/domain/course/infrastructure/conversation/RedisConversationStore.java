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
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, ConversationState.class));
        } catch (Exception e) {
            log.error("대화 상태 조회 실패 conversationId={}", conversationId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }
}
