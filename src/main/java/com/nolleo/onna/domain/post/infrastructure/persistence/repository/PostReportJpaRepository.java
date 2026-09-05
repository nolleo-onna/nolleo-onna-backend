package com.nolleo.onna.domain.post.infrastructure.persistence.repository;

import com.nolleo.onna.domain.post.infrastructure.persistence.entity.PostReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportJpaRepository extends JpaRepository<PostReportEntity, Long> {

    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);
}
