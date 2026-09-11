package com.nolleo.onna.common.infrastructure.scheduler;

import com.nolleo.onna.common.application.service.ViewCountFlushService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * 컨텍스트 종료 순서 검증 — 종료 시 동기화가 웹 서버가 멈춘 뒤, Redis 연결이 닫히기 전에 실행되는지.
 *
 * 실제 Spring 종료 절차(DefaultLifecycleProcessor)를 그대로 태운다. LettuceConnectionFactory는 시작 시 연결하지 않으므로
 * Redis 없이도 실행된다.
 */
class ViewCountFlushSchedulerShutdownTest {

    /** 서블릿 웹 서버 정지 phase (WebServerStartStopLifecycle — 패키지 전용이라 값으로 재현) */
    private static final int WEB_SERVER_STOP_PHASE = WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 1024;

    @Test
    @DisplayName("종료 시 동기화는 웹 서버가 멈춘 뒤, Redis 연결이 닫히기 전에 실행된다")
    void stop_flushesAfterWebServerStops_andBeforeRedisConnectionCloses() {
        List<String> events = new ArrayList<>();
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        ViewCountFlushService flushService = mock(ViewCountFlushService.class);
        doAnswer(invocation -> {
            events.add("flush(redisRunning=" + connectionFactory.isRunning() + ")");
            return null;
        }).when(flushService).flushAll();

        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("redisConnectionFactory", LettuceConnectionFactory.class, () -> connectionFactory);
        context.registerBean("webServer", RecordingLifecycle.class,
                () -> new RecordingLifecycle("webServerStopped", WEB_SERVER_STOP_PHASE, events));
        context.registerBean(ViewCountFlushScheduler.class, () -> new ViewCountFlushScheduler(flushService));
        context.refresh();

        context.close();

        assertThat(events).containsExactly("webServerStopped", "flush(redisRunning=true)");
        assertThat(connectionFactory.isRunning()).isFalse(); // Redis 연결도 종료 절차에서 실제로 멈췄다
    }

    /** 멈출 때 이름을 기록하는 SmartLifecycle — 웹 서버 정지 시점 표식 */
    private static final class RecordingLifecycle implements SmartLifecycle {

        private final String name;
        private final int phase;
        private final List<String> events;
        private volatile boolean running;

        private RecordingLifecycle(String name, int phase, List<String> events) {
            this.name = name;
            this.phase = phase;
            this.events = events;
        }

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            events.add(name);
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public int getPhase() {
            return phase;
        }
    }
}
