package com.nolleo.onna.domain.post.infrastructure.persistence.repository;

import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.domain.repository.PostSearchCondition;
import com.nolleo.onna.domain.post.infrastructure.persistence.entity.PostEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;

    @Override
    public Post save(Post post) {
        PostEntity entity = PostEntity.from(post);
        return postJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findActiveById(id).map(PostEntity::toDomain);
    }

    @Override
    public Page<Post> findAll(PostSearchCondition condition, Pageable pageable) {
        return postJpaRepository.findAllByCondition(
                condition.categoryTag(),
                condition.districtTag(),
                pageable
        ).map(PostEntity::toDomain);
    }

    @Override
    public List<Post> findPopular(int limit) {
        return postJpaRepository.findPopularWithImages(PageRequest.of(0, limit))
                .stream()
                .map(PostEntity::toDomain)
                .toList();
    }

    @Override
    public void incrementLikeCount(Long postId) {
        postJpaRepository.incrementLikeCount(postId);
    }

    @Override
    public void decrementLikeCount(Long postId) {
        postJpaRepository.decrementLikeCount(postId);
    }

    @Override
    public void incrementCommentCount(Long postId) {
        postJpaRepository.incrementCommentCount(postId);
    }

    @Override
    public void decrementCommentCount(Long postId) {
        postJpaRepository.decrementCommentCount(postId);
    }

    @Override
    public void incrementViewCount(Long postId) {
        postJpaRepository.incrementViewCount(postId);
    }

    @Override
    public void softDelete(Long postId, String deletedBy) {
        postJpaRepository.findActiveById(postId).ifPresent(entity -> entity.softDelete(deletedBy));
    }

    @Override
    public Post update(Long postId, Post post) {
        PostEntity entity = postJpaRepository.findActiveById(postId)
                .orElseThrow();
        entity.updateFrom(post);
        return entity.toDomain();
    }
}
