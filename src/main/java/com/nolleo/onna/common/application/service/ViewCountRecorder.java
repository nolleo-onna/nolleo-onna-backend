package com.nolleo.onna.common.application.service;

import com.nolleo.onna.common.application.port.ViewCountBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 조회 기록 — 조회수 버퍼(Redis)에 기록하고, 버퍼를 쓸 수 없으면 DB에 바로 +1 한다.
 *
 * Redis 장애가 조회 API 실패로 번지지 않고 조회수도 잃지 않게 하는 대체 경로다 (RatingCacheService와 같은 방식).
 * 대체 경로에서는 중복 조회 제한이 적용되지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j(topic = "ViewCount")
public class ViewCountRecorder {

    private final ViewCountBuffer viewCountBuffer;

    /**
     * @param dbFallback 버퍼를 쓸 수 없을 때 실행할 DB 원자 증가 (예: repository.incrementViewCount(id))
     * @return 이미 읽어 온 DB 조회수에 더해 보여줄 값 — 버퍼 경로는 DB 미반영분, 대체 경로는 방금 DB에 올린 1
     */
    public long record(String targetType, long targetId, String viewerKey, Runnable dbFallback) {
        try {
            return viewCountBuffer.recordView(targetType, targetId, viewerKey);
        } catch (DataAccessException e) {
            log.warn("조회수 버퍼 기록 실패, DB 직접 증가로 대체 - {}:{}", targetType, targetId, e);
            dbFallback.run();
            return 1L;
        }
    }
}
