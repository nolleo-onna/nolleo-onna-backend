package com.nolleo.onna.common.infrastructure.redis;

import com.nolleo.onna.common.application.port.ViewCountBuffer;
import com.nolleo.onna.common.application.port.ViewCountBuffer.Snapshot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 실제 Redis로 Lua 스크립트(중복 확인·증가, 스냅샷 분리, 되돌리기)와 RENAME 스냅샷 동작을 검증한다.
 *
 * localhost:6379에 접속할 수 없으면 전체를 건너뛴다 — 인프라 없는 환경에서도 전체 테스트가 깨지지 않는다.
 * 실행: docker compose up -d redis 후 ./gradlew test --tests "*RedisViewCountBufferIntegrationTest"
 * 대상 타입 이름을 테스트마다 무작위로 만들어 실제 데이터 키와 섞이지 않게 하고, 끝나면 해당 키를 지운다.
 */
class RedisViewCountBufferIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private RedisViewCountBuffer buffer;
    private String type;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);

        boolean available;
        try {
            available = "PONG".equals(redisTemplate.execute((RedisCallback<String>) RedisConnection::ping));
        } catch (Exception e) {
            available = false;
        }
        assumeTrue(available, "localhost:6379 Redis에 접속할 수 없어 건너뜀");
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        buffer = new RedisViewCountBuffer(redisTemplate);
        type = "it" + UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    void cleanUp() {
        Set<String> keys = redisTemplate.keys("view:*" + type + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("같은 viewer의 재조회는 세지 않고, 다른 viewer·다른 대상은 따로 센다 — 중복 표식에는 TTL이 걸린다")
    void recordView_dedupsPerViewerAndTarget() {
        assertThat(buffer.recordView(type, 1L, "u:1")).isEqualTo(1L);
        assertThat(buffer.recordView(type, 1L, "u:1")).isEqualTo(1L); // 10분 안 재조회 — 증가 없이 대기분만 읽는다
        assertThat(buffer.recordView(type, 1L, "u:2")).isEqualTo(2L);
        assertThat(buffer.recordView(type, 2L, "u:1")).isEqualTo(1L);

        Long ttl = redisTemplate.getExpire("view:dedup:" + type + ":1:u:1");
        assertThat(ttl).isBetween(1L, ViewCountBuffer.DEDUP_WINDOW.toSeconds());
    }

    @Test
    @DisplayName("detach는 대기분을 스냅샷으로 떼어내고, 이후 조회는 새 대기분에 쌓인다")
    void detach_movesPendingToSnapshot() {
        buffer.recordView(type, 1L, "u:1");
        buffer.recordView(type, 1L, "u:2");
        buffer.recordView(type, 2L, "u:1");

        Snapshot snapshot = buffer.detach(type);

        assertThat(snapshot.deltaByTargetId()).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 2L, 2L, 1L));
        assertThat(buffer.recordView(type, 1L, "u:3")).isEqualTo(1L); // 새 대기분에서 다시 시작
        assertThat(buffer.detach(type).deltaByTargetId()).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 1L));
    }

    @Test
    @DisplayName("대기분이 없으면 detach는 null이다")
    void detach_returnsNull_whenNothingPending() {
        assertThat(buffer.detach(type)).isNull();
    }

    @Test
    @DisplayName("complete하지 않은 스냅샷은 leftovers로 다시 보이고, complete하면 사라진다")
    void leftovers_returnsUncompletedSnapshots() {
        buffer.recordView(type, 1L, "u:1");
        Snapshot snapshot = buffer.detach(type);

        assertThat(buffer.leftovers(type)).extracting(Snapshot::handle).containsExactly(snapshot.handle());

        buffer.complete(snapshot);

        assertThat(buffer.leftovers(type)).isEmpty();
        assertThat(redisTemplate.hasKey(snapshot.handle())).isFalse();
    }

    @Test
    @DisplayName("restore는 스냅샷을 그사이 새로 쌓인 대기분에 더해 되돌리고 스냅샷을 정리한다")
    void restore_mergesSnapshotBackIntoPending() {
        buffer.recordView(type, 1L, "u:1");
        buffer.recordView(type, 1L, "u:2");
        Snapshot snapshot = buffer.detach(type);
        buffer.recordView(type, 1L, "u:3"); // 스냅샷 분리 후 들어온 조회

        buffer.restore(snapshot);

        assertThat(buffer.leftovers(type)).isEmpty();
        assertThat(buffer.detach(type).deltaByTargetId()).containsExactlyEntriesOf(Map.of(1L, 3L));
    }
}
