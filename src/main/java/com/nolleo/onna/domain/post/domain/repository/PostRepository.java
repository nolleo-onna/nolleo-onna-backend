package com.nolleo.onna.domain.post.domain.repository;

import com.nolleo.onna.domain.post.domain.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
    Page<Post> findAll(PostSearchCondition condition, Pageable pageable);
    List<Post> findPopular(int limit);
    void incrementLikeCount(Long postId);
    void decrementLikeCount(Long postId);
    void incrementCommentCount(Long postId);
    void decrementCommentCount(Long postId);
    void incrementViewCount(Long postId);
    void softDelete(Long postId, String deletedBy);
    Post update(Long postId, Post post);
}
