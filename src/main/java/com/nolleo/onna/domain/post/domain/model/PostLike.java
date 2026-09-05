package com.nolleo.onna.domain.post.domain.model;

import java.time.OffsetDateTime;

public record PostLike(Long id, Long postId, Long userId, OffsetDateTime createdAt) {}
