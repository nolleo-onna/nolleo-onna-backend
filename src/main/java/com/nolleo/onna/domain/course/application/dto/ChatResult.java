package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

import java.util.UUID;

/**
 * 자연어 코스 대화 결과.
 *   OFF_TOPIC              — 여행 코스 요청과 무관한 메시지, reply로 안내. 대화는 이어진다
 *   NEED_MORE_INFO         — 정보 부족, reply로 되묻기. conversationId로 대화를 이어간다
 *   COMPLETED              — 코스 생성 완료, pairId로 생성된 코스 조회
 *   LIMIT_EXCEEDED         — 일일 AI 코스 생성 횟수 초과. 대화 상태는 남아 있어 내일 같은 대화로 이어갈 수는 있다(TTL 안이면)
 *   CONVERSATION_ENDED     — 대화당 턴 상한 또는 여행 무관 메시지 연속 상한에 걸려 이 대화가 종료됨.
 *                            conversationId는 더 이상 유효하지 않으니 클라이언트는 새 대화(conversationId=null)로 시작한다
 *   MESSAGE_LIMIT_EXCEEDED — 일일 챗봇 메시지 수 초과. 파싱 전에 거절되므로 intent는 비어 있다. 내일까지 대화 불가
 */
public record ChatResult(
        Status status,
        String reply,
        String conversationId,
        CourseIntent intent,
        UUID pairId
) {
    public enum Status {
        OFF_TOPIC, NEED_MORE_INFO, COMPLETED, LIMIT_EXCEEDED, CONVERSATION_ENDED, MESSAGE_LIMIT_EXCEEDED
    }

    public static ChatResult offTopic(String reply, String conversationId, CourseIntent intent) {
        return new ChatResult(Status.OFF_TOPIC, reply, conversationId, intent, null);
    }

    public static ChatResult needMoreInfo(String reply, String conversationId, CourseIntent intent) {
        return new ChatResult(Status.NEED_MORE_INFO, reply, conversationId, intent, null);
    }

    public static ChatResult completed(String reply, String conversationId, CourseIntent intent, UUID pairId) {
        return new ChatResult(Status.COMPLETED, reply, conversationId, intent, pairId);
    }

    public static ChatResult limitExceeded(String reply, String conversationId, CourseIntent intent) {
        return new ChatResult(Status.LIMIT_EXCEEDED, reply, conversationId, intent, null);
    }

    public static ChatResult conversationEnded(String reply, String conversationId, CourseIntent intent) {
        return new ChatResult(Status.CONVERSATION_ENDED, reply, conversationId, intent, null);
    }

    public static ChatResult messageLimitExceeded(String reply, String conversationId) {
        return new ChatResult(Status.MESSAGE_LIMIT_EXCEEDED, reply, conversationId, CourseIntent.empty(), null);
    }
}
