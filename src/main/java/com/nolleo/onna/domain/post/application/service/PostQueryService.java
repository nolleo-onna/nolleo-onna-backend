package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.repository.PostLikeRepository;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.domain.repository.PostSearchCondition;
import com.nolleo.onna.domain.post.presentation.dto.response.PostDetailResponse;
import com.nolleo.onna.domain.post.presentation.dto.response.PostPopularResponse;
import com.nolleo.onna.domain.post.presentation.dto.response.PostSummaryResponse;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserJpaRepository userJpaRepository;

    @Transactional
    public PostDetailResponse getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        postRepository.incrementViewCount(postId);

        boolean isLiked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);

        UserEntity user = userJpaRepository.findById(post.getUserId()).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";
        String profileImageUrl = user != null ? user.getProfileImageUrl() : null;

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                new PostDetailResponse.AuthorInfo(nickname, profileImageUrl),
                post.getCategoryTags(),
                post.getDistrictTag(),
                post.getImageUrls(),
                post.getLikeCount(),
                isLiked,
                post.getViewCount(),
                post.getCommentCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public Page<PostSummaryResponse> getPosts(PostSearchCondition condition, Pageable pageable, Long userId) {
        Page<Post> posts = postRepository.findAll(condition, pageable);

        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();
        Set<Long> likedPostIds = userId != null
                ? postLikeRepository.findLikedPostIds(userId, postIds)
                : Set.of();

        return posts.map(post -> {
            UserEntity user = userJpaRepository.findById(post.getUserId()).orElse(null);
            String nickname = user != null ? user.getNickname() : "알 수 없음";
            String profileImageUrl = user != null ? user.getProfileImageUrl() : null;

            return new PostSummaryResponse(
                    post.getId(),
                    post.getTitle(),
                    new PostSummaryResponse.AuthorInfo(nickname, profileImageUrl),
                    post.getCategoryTags(),
                    post.getDistrictTag(),
                    post.getImageUrls() != null && !post.getImageUrls().isEmpty(),
                    post.getLikeCount(),
                    likedPostIds.contains(post.getId()),
                    post.getViewCount(),
                    post.getCommentCount(),
                    post.getCreatedAt()
            );
        });
    }

    public List<PostPopularResponse> getPopularPosts(Long userId) {
        List<Post> posts = postRepository.findPopular(5);

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Set<Long> likedPostIds = userId != null
                ? postLikeRepository.findLikedPostIds(userId, postIds)
                : Set.of();

        return posts.stream().map(post -> {
            String thumbnail = (post.getImageUrls() != null && !post.getImageUrls().isEmpty())
                    ? post.getImageUrls().get(0)
                    : null;

            return new PostPopularResponse(
                    post.getId(),
                    post.getTitle(),
                    thumbnail,
                    post.getLikeCount(),
                    likedPostIds.contains(post.getId())
            );
        }).toList();
    }
}
