package com.nolleo.onna.domain.course.application.port;

/**
 * [아웃바운드 포트] 회원별 일일 AI 코스 생성 횟수 제한.
 * 구현은 infrastructure/conversation에 위치한다.
 */
public interface CourseGenerationLimiter {

    /** 하루 최대 생성 가능 횟수 */
    int DAILY_LIMIT = 3;

    /**
     * 오늘 생성 가능한 횟수가 남아있으면 1회 소비하고 true, 이미 다 썼으면 false.
     * 원자적으로 처리되어야 한다(동시 요청에도 정확히 DAILY_LIMIT까지만 허용).
     */
    boolean tryConsume(Long userId);
}
