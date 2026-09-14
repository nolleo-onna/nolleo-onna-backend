package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.ChatResult;
import com.nolleo.onna.domain.course.application.dto.ConversationState;
import com.nolleo.onna.domain.course.application.dto.ParsedMessage;
import com.nolleo.onna.domain.course.application.port.ChatReplyWriter;
import com.nolleo.onna.domain.course.application.port.ConversationStore;
import com.nolleo.onna.domain.course.application.port.CourseGenerationLimiter;
import com.nolleo.onna.domain.course.application.port.CourseIntentParser;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 생성 확인 대기 → "코스 생성 시작" 경로의 동시성·실패 복구 규칙을 검증한다.
 * 파싱·되묻기 분기는 ChatReplyWriter/Gemini에 의존하는 문구 생성이라 여기서는 생성 경로에 집중한다.
 */
@ExtendWith(MockitoExtension.class)
class CourseChatServiceTest {

    @Mock CourseIntentParser intentParser;
    @Mock ChatReplyWriter replyWriter;
    @Mock ConversationStore conversationStore;
    @Mock CourseGenerationLimiter generationLimiter;
    @Mock CourseGenerationService courseGenerationService;

    @InjectMocks CourseChatService service;

    private static final Long USER_ID = 1L;
    private static final String CONVERSATION_ID = "conv-1";
    private static final String TRIGGER = "코스 생성 시작";

    /** 필수·선택 정보가 모두 모여 생성 확인을 이미 물어본 상태 */
    private static final CourseIntent READY_INTENT =
            new CourseIntent("광안리", false, null, "연인", List.of("로맨틱"), null, true);
    private static final ConversationState AWAITING = new ConversationState(READY_INTENT, true);

    /** 대화는 확인 대기 중이고, 사용자가 트리거 문구를 보냈다 */
    private void stubAwaitingAndTriggered() {
        given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        given(intentParser.parse(TRIGGER)).willReturn(new ParsedMessage(true, CourseIntent.empty()));
    }

    private static Course generatedCourse(UUID pairId) {
        return Course.createByAi(USER_ID, pairId, READY_INTENT, "AI_CHAT");
    }

    @Test
    @DisplayName("확인 대기 중 트리거 문구가 오면 대화 상태를 가져가고(take) 한도를 소비한 뒤 생성해 COMPLETED와 pairId를 돌려준다")
    void chat_generates_whenAwaitingAndTriggered() {
        // given
        stubAwaitingAndTriggered();
        given(conversationStore.take(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        given(generationLimiter.tryConsume(USER_ID)).willReturn(true);
        UUID pairId = UUID.randomUUID();
        given(courseGenerationService.generate(USER_ID, READY_INTENT, "AI_CHAT")).willReturn(generatedCourse(pairId));
        given(replyWriter.ready(READY_INTENT)).willReturn("광안리 코스를 만들었어요!");

        // when
        ChatResult result = service.chat(USER_ID, TRIGGER, CONVERSATION_ID);

        // then
        assertThat(result.status()).isEqualTo(ChatResult.Status.COMPLETED);
        assertThat(result.pairId()).isEqualTo(pairId);
        assertThat(result.conversationId()).isEqualTo(CONVERSATION_ID);
        // 성공하면 대화 상태를 되돌려 놓지 않는다 (take로 이미 지워졌다)
        verify(conversationStore, never()).save(anyString(), any());
        verify(generationLimiter, never()).refund(any());
    }

    @Test
    @DisplayName("같은 대화로 다른 요청이 먼저 상태를 가져갔으면 COURSE_GENERATION_IN_PROGRESS(409)로 거절하고 한도·생성을 건드리지 않는다")
    void chat_rejectsDuplicate_whenConversationAlreadyTaken() {
        // given — find 시점엔 있었지만 take 시점엔 이미 사라짐 (동시 요청이 먼저 가져감)
        stubAwaitingAndTriggered();
        given(conversationStore.take(CONVERSATION_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.chat(USER_ID, TRIGGER, CONVERSATION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_GENERATION_IN_PROGRESS);

        verifyNoInteractions(generationLimiter, courseGenerationService);
    }

    @Test
    @DisplayName("일일 한도를 다 썼으면 LIMIT_EXCEEDED를 돌려주고, 가져갔던 대화 상태를 되돌려 놓는다")
    void chat_restoresState_whenLimitExceeded() {
        // given
        stubAwaitingAndTriggered();
        given(conversationStore.take(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        given(generationLimiter.tryConsume(USER_ID)).willReturn(false);

        // when
        ChatResult result = service.chat(USER_ID, TRIGGER, CONVERSATION_ID);

        // then
        assertThat(result.status()).isEqualTo(ChatResult.Status.LIMIT_EXCEEDED);
        assertThat(result.pairId()).isNull();
        verify(conversationStore).save(CONVERSATION_ID, AWAITING);
        verifyNoInteractions(courseGenerationService);
    }

    @Test
    @DisplayName("한도 확인 자체가 실패하면(Redis 장애) 예외를 그대로 올리되 대화 상태는 되돌려 놓는다")
    void chat_restoresState_whenLimiterUnavailable() {
        // given
        stubAwaitingAndTriggered();
        given(conversationStore.take(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        given(generationLimiter.tryConsume(USER_ID))
                .willThrow(new BusinessException(CourseErrorCode.GENERATION_LIMIT_UNAVAILABLE));

        // when / then
        assertThatThrownBy(() -> service.chat(USER_ID, TRIGGER, CONVERSATION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.GENERATION_LIMIT_UNAVAILABLE);

        verify(conversationStore).save(CONVERSATION_ID, AWAITING);
        verifyNoInteractions(courseGenerationService);
    }

    @Test
    @DisplayName("생성이 실패하면 예외를 올리면서 소비한 한도를 환불하고 대화 상태를 되돌려 놓는다 — 같은 대화로 재시도할 수 있다")
    void chat_refundsAndRestoresState_whenGenerationFails() {
        // given
        stubAwaitingAndTriggered();
        given(conversationStore.take(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        given(generationLimiter.tryConsume(USER_ID)).willReturn(true);
        given(courseGenerationService.generate(USER_ID, READY_INTENT, "AI_CHAT"))
                .willThrow(new BusinessException(CourseErrorCode.NO_SPOT_CANDIDATES));

        // when / then
        assertThatThrownBy(() -> service.chat(USER_ID, TRIGGER, CONVERSATION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.NO_SPOT_CANDIDATES);

        verify(generationLimiter).refund(USER_ID);
        verify(conversationStore).save(CONVERSATION_ID, AWAITING);
    }

    @Test
    @DisplayName("환불이 실패해도 원래 생성 실패 원인이 그대로 올라간다")
    void chat_keepsOriginalError_whenRefundFails() {
        // given
        stubAwaitingAndTriggered();
        given(conversationStore.take(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        given(generationLimiter.tryConsume(USER_ID)).willReturn(true);
        given(courseGenerationService.generate(USER_ID, READY_INTENT, "AI_CHAT"))
                .willThrow(new IllegalStateException("generation boom"));
        org.mockito.BDDMockito.willThrow(new IllegalStateException("refund boom"))
                .given(generationLimiter).refund(USER_ID);

        // when / then
        assertThatThrownBy(() -> service.chat(USER_ID, TRIGGER, CONVERSATION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("generation boom");

        verify(conversationStore).save(CONVERSATION_ID, AWAITING);
    }

    @Test
    @DisplayName("확인 대기 중이라도 트리거 문구가 없으면 생성하지 않고 새 입력을 병합해 다시 확인을 묻는다")
    void chat_doesNotGenerate_withoutTriggerPhrase() {
        // given — "예산 5만원" 같은 추가 정보만 옴
        given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        CourseIntent budgetOnly = new CourseIntent(null, false, 50000, null, List.of(), null, false);
        given(intentParser.parse("예산 5만원")).willReturn(new ParsedMessage(true, budgetOnly));
        given(replyWriter.confirmGenerate(any(CourseIntent.class))).willReturn("이 조건으로 만들까요?");

        // when
        ChatResult result = service.chat(USER_ID, "예산 5만원", CONVERSATION_ID);

        // then
        assertThat(result.status()).isEqualTo(ChatResult.Status.NEED_MORE_INFO);
        assertThat(result.intent().budget()).isEqualTo(50000);
        assertThat(result.intent().startArea()).isEqualTo("광안리");
        verify(conversationStore).save(eq(CONVERSATION_ID), any(ConversationState.class));
        verify(conversationStore, never()).take(anyString());
        verifyNoInteractions(generationLimiter, courseGenerationService);
    }
}
