package com.nolleo.onna.domain.post.infrastructure.persistence.entity;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pt_post_category_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pt_post_category_tags",
                columnNames = {"post_id", "category_tag"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCategoryTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_tag", nullable = false, length = 50)
    private PostCategoryTag categoryTag;

    public PostCategoryTagEntity(PostEntity post, PostCategoryTag categoryTag) {
        this.post = post;
        this.categoryTag = categoryTag;
    }
}
