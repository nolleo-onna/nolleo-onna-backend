package com.nolleo.onna.domain.course.application.dto;

/**
 * 코스 공개 상태 전환 커맨드 — 목표 상태를 명시한다 (토글이 아니다).
 * 같은 값으로 두 번 보내도 결과가 같다(멱등).
 */
public record UpdateCourseVisibilityCommand(
        Long courseId,
        Long userId,
        boolean isPublic
) {}
