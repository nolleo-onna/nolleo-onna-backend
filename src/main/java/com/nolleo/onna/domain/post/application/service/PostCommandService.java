package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.infrastructure.s3.ImageStoragePort;
import com.nolleo.onna.domain.post.application.dto.CreatePostCommand;
import com.nolleo.onna.domain.post.application.dto.PostDetailResult;
import com.nolleo.onna.domain.post.application.dto.UpdatePostCommand;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PostCommandService {

    private final PostRepository postRepository;
    private final ImageStoragePort imageStoragePort;
    private final UserLookupPort userLookupPort;

    public PostDetailResult createPost(Long userId, CreatePostCommand command) {
        if (command.imageUrls() != null && command.imageUrls().size() > 5) {
            throw new BusinessException(PostErrorCode.TOO_MANY_IMAGES);
        }

        Post post = Post.create(
                userId,
                command.title(),
                command.content(),
                command.imageUrls() != null ? command.imageUrls() : List.of(),
                command.categoryTags(),
                command.districtTag()
        );

        Post saved = postRepository.save(post);
        return toDetailResult(saved, false);
    }

    public PostDetailResult updatePost(Long userId, Long postId, UpdatePostCommand command) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }

        if (command.imageUrls() != null && command.imageUrls().size() > 5) {
            throw new BusinessException(PostErrorCode.TOO_MANY_IMAGES);
        }

        List<String> oldImageUrls = post.getImageUrls() != null ? post.getImageUrls() : List.of();
        List<String> newImageUrls = command.imageUrls() != null ? command.imageUrls() : List.of();

        List<String> deletedUrls = new ArrayList<>(oldImageUrls);
        deletedUrls.removeAll(newImageUrls);

        post.update(command.title(), command.content(), newImageUrls, command.categoryTags(), command.districtTag());
        Post updated = postRepository.update(postId, post);

        deleteFromS3AfterCommit(deletedUrls);

        return toDetailResult(updated, false);
    }

    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }

        postRepository.softDelete(postId, userId.toString());

        List<String> urlsToDelete = post.getImageUrls() != null ? new ArrayList<>(post.getImageUrls()) : List.of();
        deleteFromS3AfterCommit(urlsToDelete);
    }

    private void deleteFromS3AfterCommit(List<String> urls) {
        if (urls.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    urls.forEach(imageStoragePort::delete);
                }
            });
        } else {
            urls.forEach(imageStoragePort::delete);
        }
    }

    private PostDetailResult toDetailResult(Post post, boolean isLiked) {
        UserLookupPort.UserProfile profile = userLookupPort.findById(post.getUserId()).orElse(null);
        String nickname = profile != null ? profile.nickname() : "알 수 없음";
        String profileImageUrl = profile != null ? profile.profileImageUrl() : null;
        return new PostDetailResult(post, nickname, profileImageUrl, isLiked);
    }
}
