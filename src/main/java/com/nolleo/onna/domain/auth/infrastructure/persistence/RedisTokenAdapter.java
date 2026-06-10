package com.nolleo.onna.domain.auth.infrastructure.persistence;

import com.nolleo.onna.domain.auth.domain.model.RefreshToken;
import com.nolleo.onna.domain.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

// [Auth] RefreshTokenRepository의 Redis 구현 — key "refresh:{userId}", value=jti, TTL=refresh 만료시간.
// StringRedisTemplate(common RedisConfig 빈) 주입. 키 만료로 자동 정리, 로그아웃 시 즉시 삭제.
@Repository
@RequiredArgsConstructor
public class RedisTokenAdapter implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if not current then
                return 0
            end
            if current ~= ARGV[1] then
                return -1
            end
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(RefreshToken refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(
                key(refreshToken.userId()),
                refreshToken.jti(),
                Duration.ofMillis(ttlMillis));
    }

    @Override
    public Optional<RefreshToken> findByUserId(Long userId) {
        String jti = redisTemplate.opsForValue().get(key(userId));
        return Optional.ofNullable(jti)
                .map(value -> RefreshToken.issue(userId, value));
    }

    @Override
    public RotationResult rotateIfMatches(Long userId, String expectedJti, String newJti, long ttlMillis) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(userId)),
                expectedJti,
                newJti,
                String.valueOf(ttlMillis));

        if (result == null || result == 0L) {
            return RotationResult.NOT_FOUND;
        }
        if (result == -1L) {
            return RotationResult.MISMATCHED;
        }
        return RotationResult.ROTATED;
    }

    @Override
    public void deleteByUserId(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
