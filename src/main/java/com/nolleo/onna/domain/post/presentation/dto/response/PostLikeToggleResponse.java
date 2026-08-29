package com.nolleo.onna.domain.post.presentation.dto.response;

public record PostLikeToggleResponse(Long postId, boolean isLiked, int likeCount) {}
