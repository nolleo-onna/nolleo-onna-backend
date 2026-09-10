package com.nolleo.onna.common.infrastructure.scheduler;

import com.nolleo.onna.common.application.service.ViewCountFlushService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 조회수 버퍼 → DB 주기 동기화 (기본 5분, app.view-count.flush-interval-ms).
 *
 * fixedDelay라 이전 실행이 끝나야 다음 실행이 시작된다. 앱 시작 직후 첫 실행에서 이전에 남은 스냅샷도 처리한다.
 * 앱 종료 직전에 한 번 더 동기화해 배포·재시작 시 대기분 유실을 줄인다.
 */
@Component
@RequiredArgsConstructor
public class ViewCountFlushScheduler {

    private final ViewCountFlushService viewCountFlushService;

    @Scheduled(fixedDelayString = "${app.view-count.flush-interval-ms:300000}")
    public void flush() {
        viewCountFlushService.flushAll();
    }

    @PreDestroy
    public void flushOnShutdown() {
        viewCountFlushService.flushAll();
    }
}
