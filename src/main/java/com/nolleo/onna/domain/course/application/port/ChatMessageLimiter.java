package com.nolleo.onna.domain.course.application.port;

/**
 * [아웃바운드 포트] 회원별 일일 챗봇 메시지 수 제한 — 비용 안전장치.
 *
 * 코스 생성 횟수(CourseGenerationLimiter)와 별개로, 생성 없이 메시지만 계속 보내는 경우의 상한이다.
 * 메시지는 한 번 받으면 파싱 비용이 이미 나간 것이라 환불 개념이 없다.
 * 구현은 infrastructure/conversation에 위치한다.
 */
public interface ChatMessageLimiter {

    /**
     * 오늘 보낼 수 있는 메시지가 남아있으면 1회 소비하고 true, 이미 dailyLimit에 도달했으면 false.
     * 원자적으로 처리되어야 한다(동시 요청에도 정확히 dailyLimit까지만 허용).
     * 한도를 확인할 수 없으면(저장소 장애) 예외를 던진다 — 비용을 셀 수 없는 상태에서 열어두지 않는다.
     */
    boolean tryConsume(Long userId, int dailyLimit);
}
