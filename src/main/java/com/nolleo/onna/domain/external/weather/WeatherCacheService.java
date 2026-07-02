package com.nolleo.onna.domain.external.weather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.external.weather.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 날씨 정보 Redis 캐시 저장/조회.
 * key: weather:{district} (예: weather:해운대구)
 * TTL: 3시간
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private static final String KEY_PREFIX = "weather:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(String district, WeatherInfo weatherInfo) {
        try {
            String value = objectMapper.writeValueAsString(weatherInfo);
            redisTemplate.opsForValue().set(KEY_PREFIX + district, value, TTL);
        } catch (JsonProcessingException e) {
            log.error("날씨 캐시 저장 실패 district={}: {}", district, e.getMessage());
        }
    }

    public Optional<WeatherInfo> get(String district) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + district);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, WeatherInfo.class));
        } catch (JsonProcessingException e) {
            log.error("날씨 캐시 조회 실패 district={}: {}", district, e.getMessage());
            return Optional.empty();
        }
    }
}
