package com.nolleo.onna.domain.comment.presentation.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        AuthorInfo author,
        String content,
        boolean deleted,
        Long parentCommentId,
        List<CommentResponse> replies,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record AuthorInfo(String nickname, String profileImageUrl) {}
}
