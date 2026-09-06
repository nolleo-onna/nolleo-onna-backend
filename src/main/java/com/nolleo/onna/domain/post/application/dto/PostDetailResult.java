package com.nolleo.onna.domain.post.application.dto;

import com.nolleo.onna.domain.post.domain.model.Post;

public record PostDetailResult(Post post, String authorNickname, String authorProfileImageUrl, boolean isLiked) {}
