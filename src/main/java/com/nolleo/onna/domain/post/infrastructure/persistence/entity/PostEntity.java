package com.nolleo.onna.domain.post.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.SoftDeleteAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pt_posts", indexes = {
        @Index(name = "idx_pt_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_pt_posts_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_pt_posts_district_tag", columnList = "district_tag")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "district_tag", length = 50)
    private PostDistrictTag districtTag;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    @Embedded
    private SoftDeleteAudit softDeleteAudit;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PostImageEntity> images = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCategoryTagEntity> categoryTags = new ArrayList<>();

    public static PostEntity from(Post domain) {
        PostEntity entity = new PostEntity();
        entity.userId = domain.getUserId();
        entity.title = domain.getTitle();
        entity.content = domain.getContent();
        entity.districtTag = domain.getDistrictTag();
        entity.likeCount = domain.getLikeCount();
        entity.commentCount = domain.getCommentCount();
        entity.viewCount = 0;
        entity.createAudit = CreateAudit.now(domain.getUserId().toString());
        entity.updateAudit = UpdateAudit.now();
        entity.softDeleteAudit = SoftDeleteAudit.active();

        if (domain.getImageUrls() != null) {
            for (int i = 0; i < domain.getImageUrls().size(); i++) {
                entity.images.add(new PostImageEntity(entity, domain.getImageUrls().get(i), (short) i));
            }
        }

        if (domain.getCategoryTags() != null) {
            for (PostCategoryTag tag : domain.getCategoryTags()) {
                entity.categoryTags.add(new PostCategoryTagEntity(entity, tag));
            }
        }

        return entity;
    }

    public void updateFrom(Post domain) {
        this.title = domain.getTitle();
        this.content = domain.getContent();
        this.districtTag = domain.getDistrictTag();

        this.images.clear();
        if (domain.getImageUrls() != null) {
            for (int i = 0; i < domain.getImageUrls().size(); i++) {
                this.images.add(new PostImageEntity(this, domain.getImageUrls().get(i), (short) i));
            }
        }

        this.categoryTags.clear();
        if (domain.getCategoryTags() != null) {
            for (PostCategoryTag tag : domain.getCategoryTags()) {
                this.categoryTags.add(new PostCategoryTagEntity(this, tag));
            }
        }

        if (this.updateAudit == null) {
            this.updateAudit = UpdateAudit.now();
        }
        this.updateAudit.touch(domain.getUserId().toString());
    }

    public void softDelete(String deletedBy) {
        if (this.softDeleteAudit == null) {
            this.softDeleteAudit = SoftDeleteAudit.active();
        }
        this.softDeleteAudit.softDelete(deletedBy);
        if (this.updateAudit == null) {
            this.updateAudit = UpdateAudit.now();
        }
        this.updateAudit.touch(deletedBy);
    }

    public Post toDomain() {
        List<String> imageUrlList = this.images.stream()
                .map(PostImageEntity::getImageUrl)
                .toList();

        List<PostCategoryTag> categoryTagList = this.categoryTags.stream()
                .map(PostCategoryTagEntity::getCategoryTag)
                .toList();

        return Post.restore(
                id, userId, title, content, imageUrlList, categoryTagList, districtTag,
                likeCount, commentCount, viewCount,
                createAudit != null ? createAudit.getCreatedAt() : null,
                updateAudit != null ? updateAudit.getUpdatedAt() : null
        );
    }
}
