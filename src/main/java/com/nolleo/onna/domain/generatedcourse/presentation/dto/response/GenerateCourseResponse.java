package com.nolleo.onna.domain.generatedcourse.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "코스 생성 응답 — 생성된 코스 묶음 UUID만 반환. 상세 조회는 GET /courses/pair/{pairId} 사용")
public record GenerateCourseResponse(

        @Schema(description = "생성된 코스 묶음 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID pairId
) {}
