package com.nolleo.onna.domain.comment.application.dto;

public record CreateCommentCommand(
        Long postId,
        Long parentCommentId,
        String content
) {}
