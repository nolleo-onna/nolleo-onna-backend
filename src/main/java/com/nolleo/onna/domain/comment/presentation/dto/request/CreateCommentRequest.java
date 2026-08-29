package com.nolleo.onna.domain.comment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(
        @NotNull Long postId,
        Long parentCommentId,
        @NotBlank String content
) {}
