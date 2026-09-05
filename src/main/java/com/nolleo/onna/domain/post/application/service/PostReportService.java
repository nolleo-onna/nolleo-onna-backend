package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.PostReport;
import com.nolleo.onna.domain.post.domain.repository.PostReportRepository;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.presentation.dto.request.ReportPostRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PostReportService {

    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;

    public void report(Long userId, Long postId, ReportPostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.CANNOT_REPORT_OWN_POST);
        }

        if (postReportRepository.existsByPostIdAndReporterId(postId, userId)) {
            throw new BusinessException(PostErrorCode.ALREADY_REPORTED);
        }

        PostReport report = new PostReport(null, postId, userId, request.reason(), OffsetDateTime.now());
        postReportRepository.save(report);
    }
}
