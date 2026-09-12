package com.nolleo.onna.domain.course.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 좋아요 토글 결과")
public record CourseLikeToggleResponse(

        @Schema(description = "토글 후 내 좋아요 상태", example = "true")
        boolean liked,

        @Schema(description = "토글 후 코스 좋아요 수 — DB 반영값", example = "8")
        int likeCount
) {}
