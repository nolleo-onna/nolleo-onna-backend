package com.nolleo.onna.domain.post.infrastructure.persistence.repository;

import com.nolleo.onna.domain.post.infrastructure.persistence.entity.PostLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeJpaRepository extends JpaRepository<PostLikeEntity, Long> {

    Optional<PostLikeEntity> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT pl.postId FROM PostLikeEntity pl WHERE pl.userId = :userId AND pl.postId IN :postIds")
    List<Long> findLikedPostIdsByUserIdAndPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
