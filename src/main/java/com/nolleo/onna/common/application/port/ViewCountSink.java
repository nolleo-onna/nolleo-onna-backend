package com.nolleo.onna.common.application.port;

import java.util.Map;

/**
 * [포트] 조회수 동기화 대상 — 도메인마다 하나씩 구현해 빈으로 등록한다.
 *
 * ViewCountFlushService가 targetType별 스냅샷을 addViewCounts로 넘긴다.
 * common은 이 인터페이스만 알고 Post · Course 같은 도메인을 직접 참조하지 않는다.
 */
public interface ViewCountSink {

    /** ViewCountBuffer.recordView에 넘기는 targetType과 같은 값 */
    String targetType();

    /**
     * 대상 id별 조회수를 DB에 더한다 (view_count = view_count + delta). 한 트랜잭션으로 반영해야 한다.
     * 그사이 삭제된 대상이 섞여 있어도 실패하지 않아야 한다.
     */
    void addViewCounts(Map<Long, Long> deltaByTargetId);
}
