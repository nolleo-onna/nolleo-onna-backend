package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.ChatResult;
import com.nolleo.onna.domain.course.application.dto.ConversationState;
import com.nolleo.onna.domain.course.application.dto.ParsedMessage;
import com.nolleo.onna.domain.course.application.port.ChatReplyWriter;
import com.nolleo.onna.domain.course.application.port.ConversationStore;
import com.nolleo.onna.domain.course.application.port.CourseIntentParser;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 자연어 코스 대화 유스케이스 조율.
 *
 * 흐름:
 *   1. 이전 대화 상태(부분 intent + 생성 확인 대기 여부) 조회 — conversationId 기준
 *   2. 이번 메시지를 Gemini로 파싱
 *   3. 여행 무관 메시지면 OFF_TOPIC 즉시 반환 (대화 상태 유지, 병합 안 함)
 *   4. 생성 확인 대기 중이었다면: "코스 생성 시작" 문구가 정확히 있을 때만 즉시 생성, 아니면 새 입력을 병합해 재평가
 *   5. 그 외에는 기존 intent와 병합 후 분기:
 *      - startArea 없음                → NEED_MORE_INFO (지역 되묻기)
 *      - 선택필드 전부 없음 & 첫 되묻기     → NEED_MORE_INFO (선호 되묻기, 1회만)
 *      - 그 외                        → NEED_MORE_INFO (생성 확인 질문 + "코스 생성 시작" 문구 안내, awaitingConfirmation=true)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseChatService {

    /** 생성 확인 대기 중, 이 문구가 메시지에 정확히 포함되어 있을 때만 코스 생성을 시작한다. */
    private static final String GENERATE_TRIGGER_PHRASE = "코스 생성 시작";

    /**
     * 비로그인 사용자를 위한 임시 테스트 userId.
     * TODO: 로그인 필수화 확정 시 실제 인증 사용자 ID로 교체하고 이 상수는 제거한다.
     */
    private static final Long TEMP_TEST_USER_ID = 1L;

    private final CourseIntentParser intentParser;
    private final ChatReplyWriter replyWriter;
    private final ConversationStore conversationStore;
    private final CourseGenerationService courseGenerationService;

    public ChatResult chat(String message, String conversationId) {
        // 1. 이전 대화 상태 복원 (없거나 만료면 새 대화)
        ConversationState previous = conversationStore.find(conversationId).orElse(null);
        String activeConversationId = (previous != null) ? conversationId : UUID.randomUUID().toString();
        CourseIntent previousIntent = previous != null ? previous.intent() : null;

        // 2. 파싱
        ParsedMessage parsed = intentParser.parse(message);

        // 여행 무관 메시지 — 기존 대화 상태를 건드리지 않고 안내만 반환 (진행 중이던 대화는 보존)
        if (!parsed.isTravelRelated()) {
            CourseIntent current = previousIntent != null ? previousIntent : CourseIntent.empty();
            return ChatResult.offTopic(replyWriter.offTopic(), activeConversationId, current);
        }

        // 3. 생성 확인 대기 중 + "코스 생성 시작" 문구 — 병합 없이 그대로 생성
        if (previous != null && previous.awaitingConfirmation() && message.contains(GENERATE_TRIGGER_PHRASE)) {
            return generate(previousIntent, activeConversationId);
        }

        // 4. 병합
        CourseIntent intent = (previousIntent != null) ? previousIntent.merge(parsed.intent()) : parsed.intent();

        // 5. 분기
        if (!intent.canGenerate()) {
            conversationStore.save(activeConversationId, new ConversationState(intent, false));
            return ChatResult.needMoreInfo(replyWriter.askStartArea(), activeConversationId, intent);
        }

        if (intent.needsClarification()) {
            CourseIntent clarified = intent.markClarified();
            conversationStore.save(activeConversationId, new ConversationState(clarified, false));
            return ChatResult.needMoreInfo(replyWriter.askPreferences(clarified), activeConversationId, clarified);
        }

        // 생성에 필요한 정보는 모두 모였음 — 바로 생성하지 않고 확인부터
        conversationStore.save(activeConversationId, new ConversationState(intent, true));
        return ChatResult.needMoreInfo(replyWriter.confirmGenerate(intent), activeConversationId, intent);
    }

    private ChatResult generate(CourseIntent intent, String conversationId) {
        conversationStore.delete(conversationId);
        Course course = courseGenerationService.generate(TEMP_TEST_USER_ID, intent, "AI_CHAT");
        return ChatResult.completed(replyWriter.ready(intent), conversationId, intent, course.getPairId());
    }
}
