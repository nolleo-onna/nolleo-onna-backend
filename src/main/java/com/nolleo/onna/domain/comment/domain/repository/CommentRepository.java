package com.nolleo.onna.domain.comment.domain.repository;

import com.nolleo.onna.domain.comment.domain.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(Long id);
    Page<Comment> findTopLevelByPostId(Long postId, Pageable pageable);
    List<Comment> findRepliesByParentIds(List<Long> parentIds);
    void update(Comment comment);
}
