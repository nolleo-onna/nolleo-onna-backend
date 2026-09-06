package com.nolleo.onna.domain.post.application.dto;

public record PostPopularResult(Long postId, String title, String thumbnail, int likeCount, boolean isLiked) {}
