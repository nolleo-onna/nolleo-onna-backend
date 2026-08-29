package com.nolleo.onna.domain.comment.infrastructure.persistence.repository;

import com.nolleo.onna.domain.comment.domain.model.Comment;
import com.nolleo.onna.domain.comment.domain.repository.CommentRepository;
import com.nolleo.onna.domain.comment.infrastructure.persistence.entity.CommentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepository {

    private final CommentJpaRepository commentJpaRepository;

    @Override
    public Comment save(Comment comment) {
        return commentJpaRepository.save(CommentEntity.from(comment)).toDomain();
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return commentJpaRepository.findById(id).map(CommentEntity::toDomain);
    }

    @Override
    public Page<Comment> findTopLevelByPostId(Long postId, Pageable pageable) {
        return commentJpaRepository.findTopLevelByPostId(postId, pageable)
                .map(CommentEntity::toDomain);
    }

    @Override
    public List<Comment> findRepliesByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) return List.of();
        return commentJpaRepository.findByParentCommentIdIn(parentIds)
                .stream()
                .map(CommentEntity::toDomain)
                .toList();
    }

    @Override
    public void update(Comment comment) {
        commentJpaRepository.findById(comment.getId()).ifPresent(entity -> entity.applyDelete(comment));
    }
}
