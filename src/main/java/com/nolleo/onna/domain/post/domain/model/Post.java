package com.nolleo.onna.domain.post.domain.model;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class Post {

    private final Long id;
    private final Long userId;
    private String title;
    private String content;
    private List<String> imageUrls;
    private List<PostCategoryTag> categoryTags;
    private PostDistrictTag districtTag;
    private int likeCount;
    private int commentCount;
    private int viewCount;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Post(Long id, Long userId, String title, String content,
                 List<String> imageUrls, List<PostCategoryTag> categoryTags,
                 PostDistrictTag districtTag, int likeCount, int commentCount, int viewCount,
                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
        this.categoryTags = categoryTags;
        this.districtTag = districtTag;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Post restore(Long id, Long userId, String title, String content,
                               List<String> imageUrls, List<PostCategoryTag> categoryTags,
                               PostDistrictTag districtTag, int likeCount, int commentCount, int viewCount,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new Post(id, userId, title, content, imageUrls, categoryTags,
                districtTag, likeCount, commentCount, viewCount, createdAt, updatedAt);
    }

    public static Post create(Long userId, String title, String content,
                              List<String> imageUrls, List<PostCategoryTag> categoryTags,
                              PostDistrictTag districtTag) {
        return new Post(null, userId, title, content, imageUrls, categoryTags,
                districtTag, 0, 0, 0, OffsetDateTime.now(), null);
    }

    public void update(String title, String content, List<String> imageUrls,
                       List<PostCategoryTag> categoryTags, PostDistrictTag districtTag) {
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
        this.categoryTags = categoryTags;
        this.districtTag = districtTag;
        this.updatedAt = OffsetDateTime.now();
    }
}
