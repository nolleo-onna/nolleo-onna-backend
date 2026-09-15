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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 자연어 코스 대화 유스케이스 조율. 회원(로그인 사용자)만 이용 가능.
 *
 * 흐름:
 *   0. 일일 메시지 상한 확인 — 파싱(Gemini 호출) 전에 거절해 초과 메시지는 비용을 쓰지 않는다
 *   1. 이전 대화 상태(부분 intent + 생성 확인 대기 여부 + 카운터) 조회 — conversationId 기준
 *   2. 이번 메시지를 Gemini로 파싱
 *   3. 여행 무관 메시지면 OFF_TOPIC 반환 (intent는 병합하지 않고 카운터만 올린다).
 *      연속 상한에 도달하면 대화 종료(CONVERSATION_ENDED)
 *   4. 생성 확인 대기 중이었다면: "코스 생성 시작" 문구가 정확히 있을 때만 대화 상태를 원자적으로 가져가(take)
 *      일일 생성 횟수 확인 후 즉시 생성 — 턴 상한과 무관하게 허용한다(대화의 목적이므로)
 *   5. 그 외에는 기존 intent와 병합 후 분기:
 *      - 턴 상한 초과                  → CONVERSATION_ENDED (대화 상태 삭제, 새 대화 안내)
 *      - startArea 없음                → NEED_MORE_INFO (지역 되묻기)
 *      - 선택필드 전부 없음 & 첫 되묻기     → NEED_MORE_INFO (선호 되묻기, 1회만. 마지막 턴이면 건너뛴다)
 *      - 그 외                        → NEED_MORE_INFO (생성 확인 질문 + "코스 생성 시작" 문구 안내, awaitingConfirmation=true)
 *
 * 비용 상한(ChatLimitPolicy)은 사용자에게 보이는 한도가 아니라 안전장치다 — 정상 사용에서는 걸리지 않는 값이어야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseChatService {

    /** 생성 확인 대기 중, 이 문구가 메시지에 정확히 포함되어 있을 때만 코스 생성을 시작한다. */
    private static final String GENERATE_TRIGGER_PHRASE = "코스 생성 시작";

    private final CourseIntentParser intentParser;
    private final ChatReplyWriter replyWriter;
    private final ConversationStore conversationStore;
    private final CourseGenerationLimiter generationLimiter;
    private final ChatMessageLimiter messageLimiter;
    private final CourseGenerationService courseGenerationService;
    private final ChatLimitPolicy policy;

    public ChatResult chat(Long userId, String message, String conversationId) {
        // 0. 일일 메시지 상한 — Gemini를 부르기 전에 확인한다
        if (!messageLimiter.tryConsume(userId, policy.dailyMessageLimit())) {
            return ChatResult.messageLimitExceeded(
                    replyWriter.messageLimitReached(policy.dailyMessageLimit()), conversationId);
        }

        // 1. 이전 대화 상태 복원 (없거나 만료면 새 대화)
        ConversationState previous = conversationStore.find(conversationId).orElse(null);
        String activeConversationId = (previous != null) ? conversationId : UUID.randomUUID().toString();
        CourseIntent previousIntent = previous != null ? previous.intent() : null;
        int turn = (previous != null ? previous.turnCount() : 0) + 1;

        // 2. 파싱
        ParsedMessage parsed = intentParser.parse(message);

        // 3. 여행 무관 메시지 — intent는 건드리지 않고 카운터만 올린다. 연속 상한이면 대화 종료
        if (!parsed.isTravelRelated()) {
            ConversationState base = previous != null ? previous : ConversationState.of(CourseIntent.empty(), false, 0);
            ConversationState next = base.afterOffTopicTurn();
            if (next.offTopicStreak() >= policy.maxOffTopicStreak()) {
                conversationStore.delete(activeConversationId);
                return ChatResult.conversationEnded(replyWriter.offTopicLimitReached(), activeConversationId, base.intent());
            }
            conversationStore.save(activeConversationId, next);
            return ChatResult.offTopic(replyWriter.offTopic(), activeConversationId, base.intent());
        }

        // 4. 생성 확인 대기 중 + "코스 생성 시작" 문구 — 대화 상태를 가져가서 일일 한도 확인 후 생성 (병합 없이 그대로)
        if (previous != null && previous.awaitingConfirmation() && message.contains(GENERATE_TRIGGER_PHRASE)) {
            return generate(userId, activeConversationId, previous);
        }

        // 5. 병합
        CourseIntent intent = (previousIntent != null) ? previousIntent.merge(parsed.intent()) : parsed.intent();

        // 턴 상한 초과 — 트리거가 아닌 메시지는 더 받지 않는다. 마지막 턴에서 이미 확인을 물었으니 남은 선택지는 생성뿐이었다
        if (turn > policy.maxTurnsPerConversation()) {
            conversationStore.delete(activeConversationId);
            return ChatResult.conversationEnded(replyWriter.turnLimitReached(), activeConversationId, intent);
        }
        boolean lastTurn = turn >= policy.maxTurnsPerConversation();

        // 6. 분기
        if (!intent.canGenerate()) {
            conversationStore.save(activeConversationId, ConversationState.of(intent, false, turn));
            return ChatResult.needMoreInfo(replyWriter.askStartArea(), activeConversationId, intent);
        }

        // 마지막 턴이면 선호 되묻기를 건너뛴다 — 되물어도 답할 턴이 없다. 지금 조건으로 생성 확인부터 받는다
        if (intent.needsClarification() && !lastTurn) {
            CourseIntent clarified = intent.markClarified();
            conversationStore.save(activeConversationId, ConversationState.of(clarified, false, turn));
            return ChatResult.needMoreInfo(replyWriter.askPreferences(clarified), activeConversationId, clarified);
        }

        // 생성에 필요한 정보는 모두 모였음 — 바로 생성하지 않고 확인부터
        conversationStore.save(activeConversationId, ConversationState.of(intent, true, turn));
        return ChatResult.needMoreInfo(replyWriter.confirmGenerate(intent), activeConversationId, intent);
    }

    /**
     * 실제 코스 생성.
     *
     * 순서: 대화 상태 소유권 획득(take) → 일일 횟수 소비 → 생성.
     *   - take가 비어 있으면 같은 대화로 다른 요청이 먼저 생성 중이다 → 409 (중복 생성·중복 한도 소비 방지)
     *   - 한도 초과·한도 확인 실패·생성 실패 시에는 가져갔던 대화 상태를 되돌려 놓는다.
     *     사용자는 같은 conversationId로 "코스 생성 시작"만 다시 보내면 재시도할 수 있다.
     *   - 생성 실패 시 소비한 횟수도 되돌린다.
     */
    private ChatResult generate(Long userId, String conversationId, ConversationState state) {
        if (conversationStore.take(conversationId).isEmpty()) {
            throw new BusinessException(CourseErrorCode.COURSE_GENERATION_IN_PROGRESS);
        }
        CourseIntent intent = state.intent();

        boolean consumed;
        try {
            consumed = generationLimiter.tryConsume(userId);
        } catch (RuntimeException e) {
            conversationStore.save(conversationId, state);
            throw e;
        }
        if (!consumed) {
            conversationStore.save(conversationId, state);
            return ChatResult.limitExceeded(
                    "오늘 AI 코스 생성 가능 횟수(하루 " + CourseGenerationLimiter.DAILY_LIMIT + "회)를 모두 사용하셨어요. 내일 다시 이용해주세요!",
                    conversationId, intent);
        }

        Course course;
        try {
            course = courseGenerationService.generate(userId, intent, "AI_CHAT");
        } catch (RuntimeException e) {
            refundQuietly(userId);
            conversationStore.save(conversationId, state);
            throw e;
        }

        // 대화 상태는 take에서 이미 지워졌다 — 성공 시 정리할 것이 없다
        return ChatResult.completed(replyWriter.ready(intent), conversationId, intent, course.getPairId());
    }

    /** 횟수 복구 실패가 원래 생성 실패 원인을 가리지 않도록 삼킨다. */
    private void refundQuietly(Long userId) {
        try {
            generationLimiter.refund(userId);
        } catch (RuntimeException e) {
            log.error("코스 생성 실패 후 일일 횟수 복구 실패 userId={}", userId, e);
        }
    }
}
