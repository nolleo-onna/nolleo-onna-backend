package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

/**
 * Redis에 저장되는 대화 진행 상태 — 부분 intent + 생성 확인 대기 여부.
 * awaitingConfirmation=true인 상태에서 사용자가 긍정 답변을 하면 그 즉시 코스를 생성한다.
 */
public record ConversationState(CourseIntent intent, boolean awaitingConfirmation) {
}
