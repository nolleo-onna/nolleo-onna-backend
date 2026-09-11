package com.nolleo.onna.domain.comment.application.dto;

public record UpdateCommentCommand(
        Long commentId,
        String content
) {}
