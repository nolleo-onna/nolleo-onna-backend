package com.nolleo.onna.domain.generatedcourse.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "코스 생성/재생성 응답")
public record GenerateCourseResponse(

        @Schema(description = "같은 입력으로 생성된 코스 묶음 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID pairId,

        @Schema(description = "코스 생성 시점 날씨 정보")
        WeatherResponse weather,

        @Schema(description = "생성된 코스 목록 (2~3개)")
        List<CourseResponse> courses
) {}
