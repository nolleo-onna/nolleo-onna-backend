package com.nolleo.onna.domain.post.presentation.dto.response;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;

import java.time.OffsetDateTime;
import java.util.List;

public record PostSummaryResponse(
        Long id,
        String title,
        AuthorInfo author,
        List<PostCategoryTag> categoryTags,
        PostDistrictTag districtTag,
        boolean hasImage,
        int likeCount,
        boolean isLiked,
        int viewCount,
        int commentCount,
        OffsetDateTime createdAt
) {
    public record AuthorInfo(String nickname, String profileImageUrl) {}
}
