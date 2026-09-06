package com.nolleo.onna.domain.comment.infrastructure.persistence.repository;

import com.nolleo.onna.domain.comment.infrastructure.persistence.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {

    Optional<CommentEntity> findByIdAndDeletedFalse(Long id);

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentCommentId IS NULL ORDER BY c.createAudit.createdAt ASC")
    Page<CommentEntity> findTopLevelByPostId(@Param("postId") Long postId, Pageable pageable);

    List<CommentEntity> findByParentCommentIdIn(List<Long> parentIds);
}
