package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

/**
 * 자연어 메시지 파싱 결과.
 * isTravelRelated=false면 intent는 빈 값이며 사용하지 않는다.
 */
public record ParsedMessage(
        boolean isTravelRelated,
        CourseIntent intent
) {}
