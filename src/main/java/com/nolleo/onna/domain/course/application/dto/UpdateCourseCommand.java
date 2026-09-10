package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;

import java.util.List;

/**
 * 코스 수정 커맨드 — 편집을 마친 제목 · 소개 · 방문 스팟 목록의 최종 상태.
 *
 * description이 null이거나 비어 있으면 소개를 지운다.
 * items에는 순번이 담기지 않는다 — 리스트에서의 위치가 곧 방문 순번이 된다.
 */
public record UpdateCourseCommand(
        Long courseId,
        Long userId,
        String title,
        String description,
        List<PlaceRef> items
) {}
