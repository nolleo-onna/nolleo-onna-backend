package com.nolleo.onna.domain.comment.domain.model;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class Comment {

    private final Long id;
    private final Long postId;
    private final Long userId;
    private final Long parentCommentId;
    private String content;
    private boolean deleted;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Comment(Long id, Long postId, Long userId, Long parentCommentId,
                    String content, boolean deleted,
                    OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Comment create(Long postId, Long userId, Long parentCommentId, String content) {
        return new Comment(null, postId, userId, parentCommentId, content, false, OffsetDateTime.now(), null);
    }

    public static Comment restore(Long id, Long postId, Long userId, Long parentCommentId,
                                  String content, boolean deleted,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new Comment(id, postId, userId, parentCommentId, content, deleted, createdAt, updatedAt);
    }

    public void softDelete() {
        this.content = "삭제된 댓글입니다.";
        this.deleted = true;
        this.updatedAt = OffsetDateTime.now();
    }
}
