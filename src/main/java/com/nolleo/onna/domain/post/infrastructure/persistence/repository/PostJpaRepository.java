package com.nolleo.onna.domain.post.infrastructure.persistence.repository;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import com.nolleo.onna.domain.post.infrastructure.persistence.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {

    @Query("SELECT p FROM PostEntity p WHERE p.id = :id AND p.softDeleteAudit.deletedAt IS NULL")
    Optional<PostEntity> findActiveById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p FROM PostEntity p
            LEFT JOIN p.categoryTags ct
            WHERE p.softDeleteAudit.deletedAt IS NULL
            AND (:categoryTag IS NULL OR ct.categoryTag = :categoryTag)
            AND (:districtTag IS NULL OR p.districtTag = :districtTag)
            ORDER BY p.createAudit.createdAt DESC
            """)
    Page<PostEntity> findAllByCondition(
            @Param("categoryTag") PostCategoryTag categoryTag,
            @Param("districtTag") PostDistrictTag districtTag,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM PostEntity p
            WHERE p.softDeleteAudit.deletedAt IS NULL
            AND EXISTS (SELECT 1 FROM PostImageEntity img WHERE img.post.id = p.id)
            ORDER BY p.likeCount DESC
            """)
    List<PostEntity> findPopularWithImages(Pageable pageable);

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount - 1 WHERE p.id = :id AND p.commentCount > 0")
    void decrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
