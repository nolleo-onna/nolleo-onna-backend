package com.nolleo.onna.common.application.service;

import com.nolleo.onna.common.application.port.ViewCountBuffer;
import com.nolleo.onna.common.application.port.ViewCountBuffer.Snapshot;
import com.nolleo.onna.common.application.port.ViewCountSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountFlushServiceTest {

    @Mock ViewCountBuffer buffer;
    @Mock ViewCountSink postSink;
    @Mock ViewCountSink courseSink;

    private ViewCountFlushService flushService;

    @BeforeEach
    void setUp() {
        given(postSink.targetType()).willReturn("post");
        flushService = new ViewCountFlushService(buffer, List.of(postSink));
    }

    private static Snapshot postSnapshot(String handle, Map<Long, Long> deltaByTargetId) {
        return new Snapshot("post", handle, deltaByTargetId);
    }

    @Test
    @DisplayName("대기분 스냅샷을 떼어내 DB에 반영하고, 성공하면 스냅샷을 제거한다")
    void flushAll_appliesSnapshot_andCompletes() {
        Snapshot snapshot = postSnapshot("s1", Map.of(1L, 3L, 2L, 1L));
        given(buffer.leftovers("post")).willReturn(List.of());
        given(buffer.detach("post")).willReturn(snapshot);

        flushService.flushAll();

        verify(postSink).addViewCounts(Map.of(1L, 3L, 2L, 1L));
        verify(buffer).complete(snapshot);
        verify(buffer, never()).restore(any());
    }

    @Test
    @DisplayName("DB 반영이 실패하면 스냅샷을 대기분으로 되돌리고 제거 처리하지 않는다")
    void flushAll_restores_whenSinkFails() {
        Snapshot snapshot = postSnapshot("s1", Map.of(1L, 3L));
        given(buffer.leftovers("post")).willReturn(List.of());
        given(buffer.detach("post")).willReturn(snapshot);
        willThrow(new IllegalStateException("DB down")).given(postSink).addViewCounts(snapshot.deltaByTargetId());

        flushService.flushAll();

        verify(buffer).restore(snapshot);
        verify(buffer, never()).complete(any());
    }

    @Test
    @DisplayName("이전 실행이 남긴 스냅샷을 새 대기분보다 먼저 반영한다")
    void flushAll_appliesLeftoversFirst() {
        Snapshot leftover = postSnapshot("old", Map.of(1L, 2L));
        Snapshot fresh = postSnapshot("new", Map.of(1L, 5L));
        given(buffer.leftovers("post")).willReturn(List.of(leftover));
        given(buffer.detach("post")).willReturn(fresh);

        flushService.flushAll();

        InOrder order = inOrder(postSink, buffer);
        order.verify(postSink).addViewCounts(leftover.deltaByTargetId());
        order.verify(buffer).complete(leftover);
        order.verify(postSink).addViewCounts(fresh.deltaByTargetId());
        order.verify(buffer).complete(fresh);
    }

    @Test
    @DisplayName("대기분이 없으면 DB에 아무것도 반영하지 않는다")
    void flushAll_doesNothing_whenNoPending() {
        given(buffer.leftovers("post")).willReturn(List.of());
        given(buffer.detach("post")).willReturn(null);

        flushService.flushAll();

        verify(postSink, never()).addViewCounts(any());
    }

    @Test
    @DisplayName("한 대상 타입의 동기화가 실패해도 다른 타입은 계속 동기화한다")
    void flushAll_continuesWithOtherTypes_whenOneTypeFails() {
        given(courseSink.targetType()).willReturn("course");
        ViewCountFlushService service = new ViewCountFlushService(buffer, List.of(postSink, courseSink));
        Snapshot courseSnapshot = new Snapshot("course", "c1", Map.of(9L, 1L));
        given(buffer.leftovers("post")).willThrow(new RedisConnectionFailureException("Redis down"));
        given(buffer.leftovers("course")).willReturn(List.of());
        given(buffer.detach("course")).willReturn(courseSnapshot);

        service.flushAll();

        verify(courseSink).addViewCounts(Map.of(9L, 1L));
        verify(buffer).complete(courseSnapshot);
    }
}
