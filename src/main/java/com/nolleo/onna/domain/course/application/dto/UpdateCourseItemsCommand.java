package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;

import java.util.List;

/**
 * 코스 방문 스팟 목록 일괄 반영 커맨드.
 *
 * items는 편집을 마친 최종 리스트다. 순번은 담기지 않는다 —
 * 리스트에서의 위치가 곧 방문 순번이 된다.
 */
public record UpdateCourseItemsCommand(
        Long courseId,
        Long userId,
        List<PlaceRef> items
) {}
