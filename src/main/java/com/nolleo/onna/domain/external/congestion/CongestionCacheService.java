package com.nolleo.onna.domain.external.congestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.external.congestion.dto.CongestionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 혼잡도 Redis 캐시.
 * key: congestion:{areaCd}:{signguCd}  TTL 24h
 * value: 구 평균 집중률 + 관광지 목록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CongestionCacheService {

    private static final String KEY_PREFIX = "congestion:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(String areaCd, String signguCd, CongestionInfo info) {
        String key = KEY_PREFIX + areaCd + ":" + signguCd;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(info), TTL);
        } catch (JsonProcessingException e) {
            log.error("혼잡도 캐시 저장 실패 key={}: {}", key, e.getMessage());
        }
    }

    public Optional<CongestionInfo> get(String areaCd, String signguCd) {
        String key = KEY_PREFIX + areaCd + ":" + signguCd;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, CongestionInfo.class));
        } catch (JsonProcessingException e) {
            log.error("혼잡도 캐시 조회 실패 key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }
}
