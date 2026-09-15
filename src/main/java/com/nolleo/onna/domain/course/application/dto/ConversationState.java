package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

/**
 * Redis에 저장되는 대화 진행 상태 — 부분 intent + 생성 확인 대기 여부 + 비용 상한용 카운터.
 * awaitingConfirmation=true인 상태에서 사용자가 "코스 생성 시작"을 보내면 그 즉시 코스를 생성한다.
 *
 * turnCount       이 대화에서 받은 사용자 메시지 수 (여행 무관 메시지 포함 — 파싱 비용은 똑같이 나간다)
 * offTopicStreak  여행 무관 메시지 연속 횟수. 여행 관련 메시지가 오면 0으로 돌아간다.
 *
 * 두 카운터는 int라 카운터가 없던 시절에 저장된 JSON({"intent":…,"awaitingConfirmation":…})도 0으로 역직렬화된다.
 */
public record ConversationState(
        CourseIntent intent,
        boolean awaitingConfirmation,
        int turnCount,
        int offTopicStreak
) {

    /** 여행 관련 턴을 반영한 상태 — 카운터를 명시적으로 넘긴다 (streak은 여행 관련 메시지가 왔으니 0) */
    public static ConversationState of(CourseIntent intent, boolean awaitingConfirmation, int turnCount) {
        return new ConversationState(intent, awaitingConfirmation, turnCount, 0);
    }

    /** 여행 무관 메시지가 온 뒤의 상태 — intent·확인 대기 여부는 그대로 두고 카운터만 올린다 */
    public ConversationState afterOffTopicTurn() {
        return new ConversationState(intent, awaitingConfirmation, turnCount + 1, offTopicStreak + 1);
    }
}
