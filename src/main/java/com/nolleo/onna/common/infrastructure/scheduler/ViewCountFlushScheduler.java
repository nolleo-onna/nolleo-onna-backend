package com.nolleo.onna.common.infrastructure.scheduler;

import com.nolleo.onna.common.application.service.ViewCountFlushService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 조회수 버퍼 → DB 주기 동기화 (기본 5분, app.view-count.flush-interval-ms).
 *
 * fixedDelay라 이전 실행이 끝나야 다음 실행이 시작된다. 앱 시작 직후 첫 실행에서 이전에 남은 스냅샷도 처리한다.
 *
 * 종료 직전 동기화는 SmartLifecycle.stop()에서 한다. 종료 시 SmartLifecycle 빈은 phase가 높은 순서로 멈추므로
 * PHASE를 웹 서버 정지보다 낮고 Redis 연결(LettuceConnectionFactory, phase 0)보다 높게 두어
 * graceful shutdown 중 처리된 조회까지 반영한 뒤 Redis 연결이 닫히게 한다.
 * (@PreDestroy는 SmartLifecycle 빈이 모두 멈춘 뒤 실행되어 Redis 호출이 실패하므로 쓰지 않는다.
 *  DB 커넥션 풀·EntityManagerFactory는 그보다 뒤인 빈 파괴 단계에서 닫히므로 이 시점에 사용할 수 있다.)
 * 진행 중이던 주기 동기화가 있으면 ViewCountFlushService의 synchronized로 그 실행이 끝난 뒤 이어서 실행된다.
 */
@Component
@RequiredArgsConstructor
public class ViewCountFlushScheduler implements SmartLifecycle {

    /** 웹 서버 graceful shutdown(SMART_LIFECYCLE_PHASE)·서버 정지(그보다 1024 낮음) 이후, Redis 연결 정지(0) 이전 */
    static final int PHASE = WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 2048;

    private final ViewCountFlushService viewCountFlushService;
    private volatile boolean running;

    @Scheduled(fixedDelayString = "${app.view-count.flush-interval-ms:300000}")
    public void flush() {
        viewCountFlushService.flushAll();
    }

    @Override
    public void start() {
        running = true;
    }

    /** 종료 직전 동기화 — 이 이후 기록된 조회는 Redis에 남아 다음 기동의 첫 동기화에서 반영된다 */
    @Override
    public void stop() {
        try {
            viewCountFlushService.flushAll();
        } finally {
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
