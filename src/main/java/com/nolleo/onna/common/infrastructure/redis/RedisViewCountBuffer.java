package com.nolleo.onna.common.infrastructure.redis;

import com.nolleo.onna.common.application.port.ViewCountBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ViewCountBuffer의 Redis 구현.
 *
 * 키 (type = post · course):
 *   view:{type}:pending              Hash    field=대상 id, value=DB 미반영 조회수 — 조회 시 HINCRBY
 *   view:{type}:flushing:{uuid}      Hash    동기화 중인 스냅샷 — pending을 RENAME으로 떼어낸 것
 *   view:{type}:flushing             Set     아직 끝나지 않은 스냅샷 키 목록 — 남은 스냅샷 재처리용
 *   view:dedup:{type}:{id}:{viewer}  String  중복 조회 방지 표식, TTL = DEDUP_WINDOW
 *
 * 설계 이유
 * - 대기분을 타입별 해시 하나에 모은다. 조회마다 갱신되는 hot key라 allkeys-lru 퇴거 대상이 되기 어렵고,
 *   RENAME 한 번으로 락 없이 원자적으로 스냅샷을 떼어낼 수 있다. 스냅샷 키 이름이 실행마다 달라 동시 실행에도 안전하다.
 * - 중복 확인 + 증가(또는 대기분 조회), 스냅샷 분리, 되돌리기는 Lua로 원자 처리한다.
 */
@Component
@RequiredArgsConstructor
public class RedisViewCountBuffer implements ViewCountBuffer {

    private static final String KEY_PREFIX = "view:";

    /** KEYS: dedup, pending / ARGV: ttlSeconds, targetId → 이 대상의 대기 조회수 */
    private static final RedisScript<Long> RECORD_VIEW = new DefaultRedisScript<>("""
            if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1]) then
              return redis.call('HINCRBY', KEYS[2], ARGV[2], 1)
            end
            local pending = redis.call('HGET', KEYS[2], ARGV[2])
            if pending then
              return tonumber(pending)
            end
            return 0
            """, Long.class);

    /** KEYS: pending, snapshot, registry → 떼어냈으면 1, 대기분이 없으면 0 */
    private static final RedisScript<Long> DETACH = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
              return 0
            end
            redis.call('RENAME', KEYS[1], KEYS[2])
            redis.call('SADD', KEYS[3], KEYS[2])
            return 1
            """, Long.class);

    /** KEYS: snapshot, pending, registry → 되돌린 대상 수 */
    private static final RedisScript<Long> RESTORE = new DefaultRedisScript<>("""
            local entries = redis.call('HGETALL', KEYS[1])
            for i = 1, #entries, 2 do
              redis.call('HINCRBY', KEYS[2], entries[i], entries[i + 1])
            end
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[3], KEYS[1])
            return #entries / 2
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public long recordView(String targetType, long targetId, String viewerKey) {
        Long pending = redisTemplate.execute(RECORD_VIEW,
                List.of(dedupKey(targetType, targetId, viewerKey), pendingKey(targetType)),
                String.valueOf(DEDUP_WINDOW.toSeconds()), String.valueOf(targetId));
        return pending != null ? pending : 0L;
    }

    @Override
    public Snapshot detach(String targetType) {
        String snapshotKey = snapshotKeyPrefix(targetType) + UUID.randomUUID();
        Long detached = redisTemplate.execute(DETACH,
                List.of(pendingKey(targetType), snapshotKey, registryKey(targetType)));
        if (detached == null || detached == 0L) {
            return null;
        }
        return read(targetType, snapshotKey);
    }

    @Override
    public List<Snapshot> leftovers(String targetType) {
        Set<String> snapshotKeys = redisTemplate.opsForSet().members(registryKey(targetType));
        if (snapshotKeys == null || snapshotKeys.isEmpty()) {
            return List.of();
        }
        List<Snapshot> snapshots = new ArrayList<>();
        for (String snapshotKey : snapshotKeys) {
            Snapshot snapshot = read(targetType, snapshotKey);
            if (snapshot.deltaByTargetId().isEmpty()) {
                // 스냅샷은 지웠지만 목록 정리 전에 멈춘 흔적 — 목록에서만 지운다
                redisTemplate.opsForSet().remove(registryKey(targetType), snapshotKey);
                continue;
            }
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @Override
    public void complete(Snapshot snapshot) {
        redisTemplate.delete(snapshot.handle());
        redisTemplate.opsForSet().remove(registryKey(snapshot.targetType()), snapshot.handle());
    }

    @Override
    public void restore(Snapshot snapshot) {
        redisTemplate.execute(RESTORE,
                List.of(snapshot.handle(), pendingKey(snapshot.targetType()), registryKey(snapshot.targetType())));
    }

    private Snapshot read(String targetType, String snapshotKey) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(snapshotKey);
        Map<Long, Long> deltaByTargetId = new HashMap<>();
        entries.forEach((targetId, delta) ->
                deltaByTargetId.put(Long.parseLong((String) targetId), Long.parseLong((String) delta)));
        return new Snapshot(targetType, snapshotKey, deltaByTargetId);
    }

    private static String pendingKey(String targetType) {
        return KEY_PREFIX + targetType + ":pending";
    }

    private static String snapshotKeyPrefix(String targetType) {
        return KEY_PREFIX + targetType + ":flushing:";
    }

    private static String registryKey(String targetType) {
        return KEY_PREFIX + targetType + ":flushing";
    }

    private static String dedupKey(String targetType, long targetId, String viewerKey) {
        return KEY_PREFIX + "dedup:" + targetType + ":" + targetId + ":" + viewerKey;
    }
}
