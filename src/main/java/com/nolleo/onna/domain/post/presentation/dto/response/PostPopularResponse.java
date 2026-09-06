package com.nolleo.onna.domain.post.presentation.dto.response;

public record PostPopularResponse(
        Long id,
        String title,
        String thumbnailImageUrl,
        int likeCount,
        boolean isLiked
) {}
