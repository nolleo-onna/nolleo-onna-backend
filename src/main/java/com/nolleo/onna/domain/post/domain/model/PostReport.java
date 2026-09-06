package com.nolleo.onna.domain.post.domain.model;

import java.time.OffsetDateTime;

public record PostReport(Long id, Long postId, Long reporterId, String reason, OffsetDateTime createdAt) {}
