package com.nolleo.onna.common.application.service;

import com.nolleo.onna.common.application.port.ViewCountBuffer;
import com.nolleo.onna.common.application.port.ViewCountBuffer.Snapshot;
import com.nolleo.onna.common.application.port.ViewCountSink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 조회수 동기화 — 버퍼에 쌓인 조회수를 대상 타입별 DB에 일괄 반영한다.
 *
 * 타입마다: 남은 스냅샷 재처리 → 대기분 스냅샷 분리(detach) → sink 반영 → 성공 시 complete, 실패 시 restore.
 * - 한 타입의 실패가 다른 타입의 동기화를 막지 않는다.
 * - DB 반영 후 complete 전에 앱이 멈추면 그 스냅샷은 다음 실행에서 다시 반영된다(드물게 중복 가산 — 조회수 특성상 허용).
 * - 주기 실행과 종료 시 실행이 겹치지 않도록 synchronized로 직렬화한다(단일 인스턴스 기준).
 */
@Service
@RequiredArgsConstructor
@Slf4j(topic = "ViewCountFlush")
public class ViewCountFlushService {

    private final ViewCountBuffer viewCountBuffer;
    private final List<ViewCountSink> sinks;

    public synchronized void flushAll() {
        for (ViewCountSink sink : sinks) {
            try {
                flush(sink);
            } catch (RuntimeException e) {
                log.error("조회수 동기화 실패 - type={}", sink.targetType(), e);
            }
        }
    }

    private void flush(ViewCountSink sink) {
        String targetType = sink.targetType();
        for (Snapshot leftover : viewCountBuffer.leftovers(targetType)) {
            apply(sink, leftover);
        }
        Snapshot snapshot = viewCountBuffer.detach(targetType);
        if (snapshot != null) {
            apply(sink, snapshot);
        }
    }

    private void apply(ViewCountSink sink, Snapshot snapshot) {
        try {
            sink.addViewCounts(snapshot.deltaByTargetId());
        } catch (RuntimeException e) {
            log.warn("조회수 DB 반영 실패, 대기분으로 되돌림 - type={}, targets={}",
                    snapshot.targetType(), snapshot.deltaByTargetId().size(), e);
            viewCountBuffer.restore(snapshot);
            return;
        }
        viewCountBuffer.complete(snapshot);
    }
}
