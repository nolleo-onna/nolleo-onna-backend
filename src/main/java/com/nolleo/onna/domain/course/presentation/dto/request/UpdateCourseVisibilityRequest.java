package com.nolleo.onna.domain.course.presentation.dto.request;

import com.nolleo.onna.domain.course.application.dto.UpdateCourseVisibilityCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 코스 공개 상태 전환 요청 — 목표 상태를 명시한다.
 * 토글이 아니므로 같은 요청을 두 번 보내도 상태가 뒤집히지 않는다.
 */
@Schema(description = "코스 공개 상태 전환 요청 — 목표 상태를 명시 (토글 아님)")
public record UpdateCourseVisibilityRequest(

        @Schema(description = "true = 공개 전환(공유 토큰 발급), false = 비공개 전환(토큰·좋아요·조회수 보존)", example = "true")
        @NotNull(message = "isPublic은 필수입니다.")
        Boolean isPublic
) {
    public UpdateCourseVisibilityCommand toCommand(Long courseId, Long userId) {
        return new UpdateCourseVisibilityCommand(courseId, userId, isPublic);
    }
}
