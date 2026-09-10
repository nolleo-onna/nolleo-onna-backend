package com.nolleo.onna.common.application.port;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * [아웃바운드 포트] 조회수 버퍼 — 조회를 빠른 저장소에 먼저 누적하고, 주기적으로 DB에 일괄 반영한다.
 *
 * 조회 경로: recordView로 기록하고, 반환된 "DB 미반영 조회수"를 DB 값에 더해 응답한다.
 * 동기화 경로(ViewCountFlushService): detach로 대기분을 스냅샷으로 떼어내 DB에 반영한 뒤 complete,
 * 반영에 실패하면 restore로 대기분에 되돌린다. 이전 실행이 끝내지 못한 스냅샷은 leftovers로 다시 처리한다.
 *
 * 구현: common/infrastructure/redis/RedisViewCountBuffer
 */
public interface ViewCountBuffer {

    /** 같은 viewer가 같은 대상을 다시 조회해도 한 번으로 치는 기간 */
    Duration DEDUP_WINDOW = Duration.ofMinutes(10);

    /**
     * 조회 1회를 기록한다. 같은 viewer의 DEDUP_WINDOW 내 재조회는 세지 않는다.
     *
     * @param targetType 대상 종류 (예: "post", "course") — ViewCountSink.targetType()과 같은 값
     * @param viewerKey  조회자 식별 키 (ViewerKeyResolver 참고)
     * @return 이 대상의 DB 미반영 조회수 (이번 조회가 집계됐다면 포함). 표시 조회수 = DB 값 + 반환값
     */
    long recordView(String targetType, long targetId, String viewerKey);

    /** 대기 중인 조회수를 스냅샷으로 떼어낸다. 이후 조회는 새 대기분에 쌓인다. 대기분이 없으면 null */
    Snapshot detach(String targetType);

    /** 이전 동기화가 끝내지 못하고 남긴 스냅샷 (DB 반영 도중 앱 종료 등) */
    List<Snapshot> leftovers(String targetType);

    /** DB 반영이 끝난 스냅샷을 제거한다 */
    void complete(Snapshot snapshot);

    /** DB 반영에 실패한 스냅샷을 대기분으로 되돌린다 — 다음 동기화에서 다시 반영된다 */
    void restore(Snapshot snapshot);

    /**
     * 떼어낸 대기 조회수 묶음.
     *
     * @param handle          구현체가 스냅샷을 가리키는 식별자(Redis 키 등) — 호출자는 해석하지 않는다
     * @param deltaByTargetId 대상 id별로 DB에 더할 조회수
     */
    record Snapshot(String targetType, String handle, Map<Long, Long> deltaByTargetId) {
        public Snapshot {
            deltaByTargetId = Map.copyOf(deltaByTargetId);
        }
    }
}
