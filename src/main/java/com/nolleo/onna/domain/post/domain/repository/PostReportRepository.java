package com.nolleo.onna.domain.post.domain.repository;

import com.nolleo.onna.domain.post.domain.model.PostReport;

public interface PostReportRepository {
    PostReport save(PostReport report);
    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);
}
