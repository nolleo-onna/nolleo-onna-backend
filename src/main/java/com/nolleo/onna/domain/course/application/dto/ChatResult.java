package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

import java.util.UUID;

/**
 * 자연어 코스 대화 결과.
 *   OFF_TOPIC      — 여행 코스 요청과 무관한 메시지, reply로 안내
 *   NEED_MORE_INFO — 정보 부족, reply로 되묻기
 *   COMPLETED      — 코스 생성 완료, pairId로 생성된 코스 조회
 */
public record ChatResult(
        Status status,
        String reply,
        String conversationId,
        CourseIntent intent,
        UUID pairId
) {
    public enum Status { OFF_TOPIC, NEED_MORE_INFO, COMPLETED }

    public static ChatResult offTopic(String reply, String conversationId, CourseIntent intent) {
        return new ChatResult(Status.OFF_TOPIC, reply, conversationId, intent, null);
    }

    public static ChatResult needMoreInfo(String reply, String conversationId, CourseIntent intent) {
        return new ChatResult(Status.NEED_MORE_INFO, reply, conversationId, intent, null);
    }

    public static ChatResult completed(String reply, String conversationId, CourseIntent intent, UUID pairId) {
        return new ChatResult(Status.COMPLETED, reply, conversationId, intent, pairId);
    }
}
