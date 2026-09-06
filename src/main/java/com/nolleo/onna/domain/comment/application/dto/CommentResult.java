package com.nolleo.onna.domain.comment.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CommentResult(
        Long id,
        String authorNickname,
        String authorProfileImageUrl,
        String content,
        boolean deleted,
        Long parentCommentId,
        List<CommentResult> replies,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
