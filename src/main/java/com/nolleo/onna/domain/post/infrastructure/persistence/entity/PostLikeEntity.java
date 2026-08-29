package com.nolleo.onna.domain.post.infrastructure.persistence.entity;

import com.nolleo.onna.domain.post.domain.model.PostLike;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pt_post_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pt_post_likes",
                columnNames = {"post_id", "user_id"}),
        indexes = {
                @Index(name = "idx_pt_post_likes_post_id", columnList = "post_id"),
                @Index(name = "idx_pt_post_likes_user_id", columnList = "user_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static PostLikeEntity from(PostLike domain) {
        PostLikeEntity entity = new PostLikeEntity();
        entity.postId = domain.postId();
        entity.userId = domain.userId();
        entity.createdAt = domain.createdAt() != null ? domain.createdAt() : OffsetDateTime.now();
        return entity;
    }

    public PostLike toDomain() {
        return new PostLike(id, postId, userId, createdAt);
    }
}
