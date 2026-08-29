package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.infrastructure.s3.ImageStoragePort;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.presentation.dto.request.CreatePostRequest;
import com.nolleo.onna.domain.post.presentation.dto.request.UpdatePostRequest;
import com.nolleo.onna.domain.post.presentation.dto.response.PostDetailResponse;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PostCommandService {

    private final PostRepository postRepository;
    private final ImageStoragePort imageStoragePort;
    private final UserJpaRepository userJpaRepository;

    public PostDetailResponse createPost(Long userId, CreatePostRequest request) {
        if (request.imageUrls() != null && request.imageUrls().size() > 5) {
            throw new BusinessException(PostErrorCode.TOO_MANY_IMAGES);
        }

        Post post = Post.create(
                userId,
                request.title(),
                request.content(),
                request.imageUrls() != null ? request.imageUrls() : List.of(),
                request.categoryTags(),
                request.districtTag()
        );

        Post saved = postRepository.save(post);
        return toDetailResponse(saved, false);
    }

    public PostDetailResponse updatePost(Long userId, Long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }

        if (request.imageUrls() != null && request.imageUrls().size() > 5) {
            throw new BusinessException(PostErrorCode.TOO_MANY_IMAGES);
        }

        // 삭제된 이미지 S3에서 제거
        List<String> oldImageUrls = post.getImageUrls() != null ? post.getImageUrls() : List.of();
        List<String> newImageUrls = request.imageUrls() != null ? request.imageUrls() : List.of();

        List<String> deletedUrls = new ArrayList<>(oldImageUrls);
        deletedUrls.removeAll(newImageUrls);
        for (String url : deletedUrls) {
            imageStoragePort.delete(url);
        }

        post.update(request.title(), request.content(), newImageUrls, request.categoryTags(), request.districtTag());

        Post updated = postRepository.update(postId, post);
        return toDetailResponse(updated, false);
    }

    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }

        // S3 이미지 삭제
        if (post.getImageUrls() != null) {
            for (String url : post.getImageUrls()) {
                imageStoragePort.delete(url);
            }
        }

        // soft delete는 엔티티 레벨에서 처리 (JPA로 직접)
        // PostRepository를 통해 soft delete 처리
        postRepository.softDelete(postId, userId.toString());
    }

    private PostDetailResponse toDetailResponse(Post post, boolean isLiked) {
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
}
