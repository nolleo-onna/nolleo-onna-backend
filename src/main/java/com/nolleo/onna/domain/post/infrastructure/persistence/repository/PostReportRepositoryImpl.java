package com.nolleo.onna.domain.post.infrastructure.persistence.repository;

import com.nolleo.onna.domain.post.domain.model.PostReport;
import com.nolleo.onna.domain.post.domain.repository.PostReportRepository;
import com.nolleo.onna.domain.post.infrastructure.persistence.entity.PostReportEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostReportRepositoryImpl implements PostReportRepository {

    private final PostReportJpaRepository postReportJpaRepository;

    @Override
    public PostReport save(PostReport report) {
        return postReportJpaRepository.save(PostReportEntity.from(report)).toDomain();
    }

    @Override
    public boolean existsByPostIdAndReporterId(Long postId, Long reporterId) {
        return postReportJpaRepository.existsByPostIdAndReporterId(postId, reporterId);
    }
}
