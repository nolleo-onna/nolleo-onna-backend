package com.nolleo.onna.domain.generatedcourse.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "pairId로 묶인 코스 목록 응답")
public record PairCourseResponse(

        @Schema(description = "코스 묶음 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID pairId,

        @Schema(description = "해당 묶음의 코스 목록")
        List<CourseResponse> courses
) {}
