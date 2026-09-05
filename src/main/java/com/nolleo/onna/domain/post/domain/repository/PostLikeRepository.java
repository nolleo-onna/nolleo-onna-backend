package com.nolleo.onna.domain.post.domain.repository;

import com.nolleo.onna.domain.post.domain.model.PostLike;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository {
    PostLike save(PostLike like);
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void delete(PostLike like);
    Set<Long> findLikedPostIds(Long userId, List<Long> postIds);
}
