package com.nolleo.onna.domain.post.presentation.dto.response;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;

import java.time.OffsetDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        AuthorInfo author,
        List<PostCategoryTag> categoryTags,
        PostDistrictTag districtTag,
        List<String> imageUrls,
        int likeCount,
        boolean isLiked,
        int viewCount,
        int commentCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record AuthorInfo(String nickname, String profileImageUrl) {}
}
