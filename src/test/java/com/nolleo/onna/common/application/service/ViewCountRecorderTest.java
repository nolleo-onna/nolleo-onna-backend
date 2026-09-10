package com.nolleo.onna.common.application.service;

import com.nolleo.onna.common.application.port.ViewCountBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ViewCountRecorderTest {

    @Mock ViewCountBuffer viewCountBuffer;

    @InjectMocks ViewCountRecorder viewCountRecorder;

    @Test
    @DisplayName("버퍼 기록에 성공하면 버퍼의 대기 조회수를 돌려주고 DB 대체 경로는 실행하지 않는다")
    void record_returnsPendingFromBuffer() {
        given(viewCountBuffer.recordView("post", 1L, "u:7")).willReturn(4L);
        AtomicInteger fallbackCalls = new AtomicInteger();

        long pending = viewCountRecorder.record("post", 1L, "u:7", fallbackCalls::incrementAndGet);

        assertThat(pending).isEqualTo(4L);
        assertThat(fallbackCalls).hasValue(0);
    }

    @Test
    @DisplayName("버퍼가 Redis 장애로 실패하면 DB 대체 경로를 실행하고, 방금 DB에 올린 1건을 돌려준다")
    void record_fallsBackToDb_whenBufferUnavailable() {
        given(viewCountBuffer.recordView("post", 1L, "u:7"))
                .willThrow(new RedisConnectionFailureException("Redis down"));
        AtomicInteger fallbackCalls = new AtomicInteger();

        long pending = viewCountRecorder.record("post", 1L, "u:7", fallbackCalls::incrementAndGet);

        assertThat(pending).isEqualTo(1L);
        assertThat(fallbackCalls).hasValue(1);
    }
}
