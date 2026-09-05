package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.PostLike;
import com.nolleo.onna.domain.post.domain.repository.PostLikeRepository;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.presentation.dto.response.PostLikeToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    public PostLikeToggleResponse toggleLike(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            postRepository.decrementLikeCount(postId);
            int likeCount = Math.max(0, post.getLikeCount() - 1);
            return new PostLikeToggleResponse(postId, false, likeCount);
        } else {
            PostLike newLike = new PostLike(null, postId, userId, OffsetDateTime.now());
            postLikeRepository.save(newLike);
            postRepository.incrementLikeCount(postId);
            int likeCount = post.getLikeCount() + 1;
            return new PostLikeToggleResponse(postId, true, likeCount);
        }
    }
}
