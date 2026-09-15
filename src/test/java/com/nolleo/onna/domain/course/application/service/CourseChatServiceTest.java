package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.ChatLimitPolicy;
import com.nolleo.onna.domain.course.application.dto.ChatResult;
import com.nolleo.onna.domain.course.application.dto.ConversationState;
import com.nolleo.onna.domain.course.application.dto.ParsedMessage;
import com.nolleo.onna.domain.course.application.port.ChatMessageLimiter;
import com.nolleo.onna.domain.course.application.port.ChatReplyWriter;
import com.nolleo.onna.domain.course.application.port.ConversationStore;
import com.nolleo.onna.domain.course.application.port.CourseGenerationLimiter;
import com.nolleo.onna.domain.course.application.port.CourseIntentParser;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * 생성 확인 대기 → "코스 생성 시작" 경로의 동시성·실패 복구 규칙과, 비용 상한(ChatLimitPolicy) 규칙을 검증한다.
 * 파싱·되묻기 문구 자체는 ChatReplyWriter/Gemini 책임이라 여기서는 분기와 상태 저장만 본다.
 */
@ExtendWith(MockitoExtension.class)
class CourseChatServiceTest {

    @Mock CourseIntentParser intentParser;
    @Mock ChatReplyWriter replyWriter;
    @Mock ConversationStore conversationStore;
    @Mock CourseGenerationLimiter generationLimiter;
    @Mock ChatMessageLimiter messageLimiter;
    @Mock CourseGenerationService courseGenerationService;

    private static final int MAX_TURNS = 10;
    private static final int MAX_OFF_TOPIC_STREAK = 3;
    private static final int DAILY_MESSAGE_LIMIT = 40;

    private CourseChatService service;

    @BeforeEach
    void setUp() {
        service = new CourseChatService(intentParser, replyWriter, conversationStore, generationLimiter,
                messageLimiter, courseGenerationService,
                new ChatLimitPolicy(MAX_TURNS, MAX_OFF_TOPIC_STREAK, DAILY_MESSAGE_LIMIT));
    }

    private static final Long USER_ID = 1L;
    private static final String CONVERSATION_ID = "conv-1";
    private static final String TRIGGER = "코스 생성 시작";

    /** 필수·선택 정보가 모두 모여 생성 확인을 이미 물어본 상태 (3번째 턴) */
    private static final CourseIntent READY_INTENT =
            new CourseIntent("광안리", false, null, "연인", List.of("로맨틱"), null, true);
    private static final ConversationState AWAITING = ConversationState.of(READY_INTENT, true, 3);

    private static final ParsedMessage OFF_TOPIC = new ParsedMessage(false, CourseIntent.empty());

    /** 일일 메시지 한도가 남아 있다 — 파싱 전에 매번 확인하므로 거의 모든 테스트가 필요로 한다 */
    private void allowMessages() {
        given(messageLimiter.tryConsume(USER_ID, DAILY_MESSAGE_LIMIT)).willReturn(true);
    }

    /** 대화는 확인 대기 중이고, 사용자가 트리거 문구를 보냈다 */
    private void stubAwaitingAndTriggered() {
        allowMessages();
        given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
        given(intentParser.parse(TRIGGER)).willReturn(new ParsedMessage(true, CourseIntent.empty()));
    }

    private static Course generatedCourse(UUID pairId) {
        return Course.createByAi(USER_ID, pairId, READY_INTENT, "AI_CHAT");
    }

    private ConversationState savedState() {
        ArgumentCaptor<ConversationState> captor = ArgumentCaptor.forClass(ConversationState.class);
        verify(conversationStore).save(eq(CONVERSATION_ID), captor.capture());
        return captor.getValue();
    }

    // ── 생성 경로 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("생성 경로")
    class Generation {

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
        @DisplayName("트리거는 턴 상한을 넘긴 뒤에도 허용된다 — 마지막 턴에 확인을 물었으면 그 답이 상한 다음 턴이기 때문")
        void chat_allowsTrigger_evenPastTurnLimit() {
            // given — 마지막 턴(10)에 확인을 물어둔 상태, 11번째 메시지가 트리거
            ConversationState awaitingAtLastTurn = ConversationState.of(READY_INTENT, true, MAX_TURNS);
            allowMessages();
            given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(awaitingAtLastTurn));
            given(intentParser.parse(TRIGGER)).willReturn(new ParsedMessage(true, CourseIntent.empty()));
            given(conversationStore.take(CONVERSATION_ID)).willReturn(Optional.of(awaitingAtLastTurn));
            given(generationLimiter.tryConsume(USER_ID)).willReturn(true);
            given(courseGenerationService.generate(USER_ID, READY_INTENT, "AI_CHAT")).willReturn(generatedCourse(UUID.randomUUID()));
            given(replyWriter.ready(READY_INTENT)).willReturn("완료");

            // when
            ChatResult result = service.chat(USER_ID, TRIGGER, CONVERSATION_ID);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.COMPLETED);
            verify(conversationStore, never()).delete(anyString());
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
        @DisplayName("일일 생성 한도를 다 썼으면 LIMIT_EXCEEDED를 돌려주고, 가져갔던 대화 상태를 되돌려 놓는다")
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
        @DisplayName("확인 대기 중이라도 트리거 문구가 없으면 생성하지 않고 새 입력을 병합해 다시 확인을 묻는다 — 턴은 1 증가")
        void chat_doesNotGenerate_withoutTriggerPhrase() {
            // given — "예산 5만원" 같은 추가 정보만 옴
            allowMessages();
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
            ConversationState saved = savedState();
            assertThat(saved.awaitingConfirmation()).isTrue();
            assertThat(saved.turnCount()).isEqualTo(AWAITING.turnCount() + 1);
            verify(conversationStore, never()).take(anyString());
            verifyNoInteractions(generationLimiter, courseGenerationService);
        }
    }

    // ── 비용 상한 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("비용 상한 (ChatLimitPolicy)")
    class CostLimits {

        @Test
        @DisplayName("일일 메시지 상한에 도달하면 파싱(Gemini)도 대화 조회도 하지 않고 MESSAGE_LIMIT_EXCEEDED를 돌려준다")
        void chat_rejectsBeforeParsing_whenDailyMessageLimitReached() {
            // given
            given(messageLimiter.tryConsume(USER_ID, DAILY_MESSAGE_LIMIT)).willReturn(false);
            given(replyWriter.messageLimitReached(DAILY_MESSAGE_LIMIT)).willReturn("내일 다시 이용해주세요");

            // when
            ChatResult result = service.chat(USER_ID, "광안리 가고 싶어", CONVERSATION_ID);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.MESSAGE_LIMIT_EXCEEDED);
            assertThat(result.conversationId()).isEqualTo(CONVERSATION_ID);
            assertThat(result.intent()).isEqualTo(CourseIntent.empty());
            verifyNoInteractions(intentParser, conversationStore, generationLimiter, courseGenerationService);
        }

        @Test
        @DisplayName("메시지 한도 확인이 실패하면(Redis 장애) 예외를 그대로 올린다 — 비용을 셀 수 없으면 열어두지 않는다")
        void chat_propagates_whenMessageLimiterUnavailable() {
            // given
            given(messageLimiter.tryConsume(USER_ID, DAILY_MESSAGE_LIMIT))
                    .willThrow(new BusinessException(CourseErrorCode.CHAT_LIMIT_UNAVAILABLE));

            // when / then
            assertThatThrownBy(() -> service.chat(USER_ID, "광안리", CONVERSATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.CHAT_LIMIT_UNAVAILABLE);

            verifyNoInteractions(intentParser, conversationStore);
        }

        @Test
        @DisplayName("턴 상한을 넘긴 메시지가 트리거가 아니면 대화를 삭제하고 CONVERSATION_ENDED를 돌려준다")
        void chat_endsConversation_whenTurnLimitExceeded() {
            // given — 이미 10턴을 쓴 대화에 11번째 일반 메시지
            ConversationState atLimit = ConversationState.of(READY_INTENT, true, MAX_TURNS);
            allowMessages();
            given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(atLimit));
            given(intentParser.parse("음 잠깐만")).willReturn(new ParsedMessage(true, CourseIntent.empty()));
            given(replyWriter.turnLimitReached()).willReturn("새 대화로 시작해주세요");

            // when
            ChatResult result = service.chat(USER_ID, "음 잠깐만", CONVERSATION_ID);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.CONVERSATION_ENDED);
            assertThat(result.intent().startArea()).isEqualTo("광안리"); // 병합된 intent는 참고용으로 돌려준다
            verify(conversationStore).delete(CONVERSATION_ID);
            verify(conversationStore, never()).save(anyString(), any());
            verifyNoInteractions(generationLimiter, courseGenerationService);
        }

        @Test
        @DisplayName("마지막 턴에서는 선호 되묻기를 건너뛰고 바로 생성 확인을 묻는다 — 되물어도 답할 턴이 없다")
        void chat_skipsClarification_onLastTurn() {
            // given — 9턴을 썼고 지역만 있는 intent (선택 필드 전부 없음 → 평소라면 되묻기)
            CourseIntent areaOnly = new CourseIntent("광안리", false, null, null, List.of(), null, false);
            allowMessages();
            given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(ConversationState.of(areaOnly, false, MAX_TURNS - 1)));
            given(intentParser.parse("그냥 추천해줘")).willReturn(new ParsedMessage(true, CourseIntent.empty()));
            given(replyWriter.confirmGenerate(areaOnly)).willReturn("이 조건으로 만들까요?");

            // when
            ChatResult result = service.chat(USER_ID, "그냥 추천해줘", CONVERSATION_ID);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.NEED_MORE_INFO);
            ConversationState saved = savedState();
            assertThat(saved.awaitingConfirmation()).isTrue();
            assertThat(saved.turnCount()).isEqualTo(MAX_TURNS);
            verify(replyWriter, never()).askPreferences(any());
        }

        @Test
        @DisplayName("마지막 턴이 아니면 선택 필드가 전부 없을 때 선호를 1회 되묻는다 (기존 동작 유지)")
        void chat_asksPreferences_beforeLastTurn() {
            // given
            CourseIntent areaOnly = new CourseIntent("광안리", false, null, null, List.of(), null, false);
            allowMessages();
            given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(ConversationState.of(areaOnly, false, 1)));
            given(intentParser.parse("응")).willReturn(new ParsedMessage(true, CourseIntent.empty()));
            given(replyWriter.askPreferences(any(CourseIntent.class))).willReturn("누구와 가시나요?");

            // when
            ChatResult result = service.chat(USER_ID, "응", CONVERSATION_ID);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.NEED_MORE_INFO);
            assertThat(result.intent().clarifiedOnce()).isTrue();
            assertThat(savedState().turnCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("여행 무관 메시지는 intent를 건드리지 않고 턴·연속 횟수만 올려 저장한 뒤 OFF_TOPIC을 돌려준다")
        void chat_countsOffTopic_withoutTouchingIntent() {
            // given — 확인 대기 중인 대화에 잡담 1회
            allowMessages();
            given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(AWAITING));
            given(intentParser.parse("오늘 날씨 어때?")).willReturn(OFF_TOPIC);
            given(replyWriter.offTopic()).willReturn("여행 코스 챗봇이에요!");

            // when
            ChatResult result = service.chat(USER_ID, "오늘 날씨 어때?", CONVERSATION_ID);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.OFF_TOPIC);
            assertThat(result.intent()).isEqualTo(READY_INTENT);
            ConversationState saved = savedState();
            assertThat(saved.intent()).isEqualTo(READY_INTENT);
            assertThat(saved.awaitingConfirmation()).isTrue();
            assertThat(saved.turnCount()).isEqualTo(AWAITING.turnCount() + 1);
            assertThat(saved.offTopicStreak()).isEqualTo(1);
            verify(conversationStore, never()).delete(anyString());
        }

        @Test
        @DisplayName("여행 무관 메시지가 연속 상한에 도달하면 대화를 삭제하고 CONVERSATION_ENDED를 돌려준다")
        void chat_endsConversation_whenOffTopicStreakReached() {
            // given — 이미 2회 연속 잡담, 3번째 잡담
            ConversationState twoStrikes = new ConversationState(READY_INTENT, true, 5, MAX_OFF_TOPIC_STREAK - 1);
            allowMessages();
            given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(twoStrikes));
            given(intentParser.parse("ㅋㅋㅋ")).willReturn(OFF_TOPIC);
            given(replyWriter.offTopicLimitReached()).willReturn("대화를 마칠게요");

            // when
            ChatResult result = service.chat(USER_ID, "ㅋㅋㅋ", CONVERSATION_ID);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.CONVERSATION_ENDED);
            verify(conversationStore).delete(CONVERSATION_ID);
            verify(conversationStore, never()).save(anyString(), any());
        }

        @Test
        @DisplayName("여행 관련 메시지가 오면 연속 잡담 횟수는 0으로 돌아간다")
        void chat_resetsOffTopicStreak_onTravelMessage() {
            // given — 잡담 2회 뒤 여행 메시지
            CourseIntent areaOnly = new CourseIntent("광안리", false, null, null, List.of(), null, true);
            ConversationState twoStrikes = new ConversationState(areaOnly, false, 4, 2);
            allowMessages();
            given(conversationStore.find(CONVERSATION_ID)).willReturn(Optional.of(twoStrikes));
            CourseIntent companion = new CourseIntent(null, false, null, "친구", List.of(), null, false);
            given(intentParser.parse("친구랑 가")).willReturn(new ParsedMessage(true, companion));
            given(replyWriter.confirmGenerate(any(CourseIntent.class))).willReturn("만들까요?");

            // when
            service.chat(USER_ID, "친구랑 가", CONVERSATION_ID);

            // then
            ConversationState saved = savedState();
            assertThat(saved.offTopicStreak()).isZero();
            assertThat(saved.turnCount()).isEqualTo(5);
            assertThat(saved.intent().companion()).isEqualTo("친구");
        }

        @Test
        @DisplayName("새 대화의 첫 메시지가 잡담이어도 새 conversationId로 상태를 만들어 연속 횟수를 세기 시작한다")
        void chat_startsCountingOffTopic_onNewConversation() {
            // given
            allowMessages();
            given(conversationStore.find(null)).willReturn(Optional.empty());
            given(intentParser.parse("안녕")).willReturn(OFF_TOPIC);
            given(replyWriter.offTopic()).willReturn("여행 코스 챗봇이에요!");

            // when
            ChatResult result = service.chat(USER_ID, "안녕", null);

            // then
            assertThat(result.status()).isEqualTo(ChatResult.Status.OFF_TOPIC);
            assertThat(result.conversationId()).isNotBlank();
            ArgumentCaptor<ConversationState> captor = ArgumentCaptor.forClass(ConversationState.class);
            verify(conversationStore).save(eq(result.conversationId()), captor.capture());
            assertThat(captor.getValue().turnCount()).isEqualTo(1);
            assertThat(captor.getValue().offTopicStreak()).isEqualTo(1);
        }
    }
}
