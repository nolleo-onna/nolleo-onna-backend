package com.nolleo.onna.domain.post.infrastructure.persistence.repository;

import com.nolleo.onna.domain.post.domain.model.PostLike;
import com.nolleo.onna.domain.post.domain.repository.PostLikeRepository;
import com.nolleo.onna.domain.post.infrastructure.persistence.entity.PostLikeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PostLikeRepositoryImpl implements PostLikeRepository {

    private final PostLikeJpaRepository postLikeJpaRepository;

    @Override
    public PostLike save(PostLike like) {
        return postLikeJpaRepository.save(PostLikeEntity.from(like)).toDomain();
    }

    @Override
    public Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId) {
        return postLikeJpaRepository.findByPostIdAndUserId(postId, userId)
                .map(PostLikeEntity::toDomain);
    }

    @Override
    public boolean existsByPostIdAndUserId(Long postId, Long userId) {
        return postLikeJpaRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    public void delete(PostLike like) {
        postLikeJpaRepository.deleteById(like.id());
    }

    @Override
    public Set<Long> findLikedPostIds(Long userId, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(postLikeJpaRepository.findLikedPostIdsByUserIdAndPostIds(userId, postIds));
    }
}
