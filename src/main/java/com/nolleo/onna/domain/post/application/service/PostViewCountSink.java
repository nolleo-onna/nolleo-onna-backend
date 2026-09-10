package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.application.port.ViewCountSink;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 게시글 조회수를 DB(pt_posts.view_count)에 일괄 반영하는 조회수 동기화 대상 */
@Service
@RequiredArgsConstructor
public class PostViewCountSink implements ViewCountSink {

    /** 조회수 버퍼의 게시글 대상 타입 — PostQueryService.getPost의 기록과 같은 값이어야 한다 */
    public static final String TARGET_TYPE = "post";

    private final PostRepository postRepository;

    @Override
    public String targetType() {
        return TARGET_TYPE;
    }

    @Override
    @Transactional
    public void addViewCounts(Map<Long, Long> deltaByTargetId) {
        postRepository.addViewCounts(deltaByTargetId);
    }
}
