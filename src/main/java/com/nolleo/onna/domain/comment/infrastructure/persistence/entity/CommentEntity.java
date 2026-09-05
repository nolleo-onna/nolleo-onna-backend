package com.nolleo.onna.domain.comment.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.comment.domain.model.Comment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cm_comments", indexes = {
        @Index(name = "idx_cm_comments_post_id", columnList = "post_id"),
        @Index(name = "idx_cm_comments_parent_id", columnList = "parent_comment_id"),
        @Index(name = "idx_cm_comments_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    public static CommentEntity from(Comment domain) {
        CommentEntity entity = new CommentEntity();
        entity.postId = domain.getPostId();
        entity.userId = domain.getUserId();
        entity.parentCommentId = domain.getParentCommentId();
        entity.content = domain.getContent();
        entity.deleted = domain.isDeleted();
        entity.createAudit = CreateAudit.now(domain.getUserId().toString());
        entity.updateAudit = UpdateAudit.now();
        return entity;
    }

    public void applyDelete(Comment domain) {
        this.content = domain.getContent();
        this.deleted = domain.isDeleted();
        if (this.updateAudit == null) {
            this.updateAudit = UpdateAudit.now();
        }
        this.updateAudit.touch(domain.getUserId().toString());
    }

    public Comment toDomain() {
        return Comment.restore(
                id, postId, userId, parentCommentId, content, deleted,
                createAudit != null ? createAudit.getCreatedAt() : null,
                updateAudit != null ? updateAudit.getUpdatedAt() : null
        );
    }
}
