package com.nolleo.onna.domain.comment.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.comment.domain.exception.CommentErrorCode;
import com.nolleo.onna.domain.comment.domain.model.Comment;
import com.nolleo.onna.domain.comment.domain.repository.CommentRepository;
import com.nolleo.onna.domain.comment.presentation.dto.request.CreateCommentRequest;
import com.nolleo.onna.domain.comment.presentation.dto.response.CommentResponse;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentCommandService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserJpaRepository userJpaRepository;

    public CommentResponse createComment(Long userId, CreateCommentRequest request) {
        postRepository.findById(request.postId())
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

            if (parent.getParentCommentId() != null) {
                throw new BusinessException(CommentErrorCode.INVALID_PARENT_COMMENT);
            }
        }

        Comment comment = Comment.create(request.postId(), userId, request.parentCommentId(), request.content());
        Comment saved = commentRepository.save(comment);

        postRepository.incrementCommentCount(request.postId());

        UserEntity user = userJpaRepository.findById(userId).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";
        String profileImageUrl = user != null ? user.getProfileImageUrl() : null;

        return new CommentResponse(
                saved.getId(),
                new CommentResponse.AuthorInfo(nickname, profileImageUrl),
                saved.getContent(),
                saved.isDeleted(),
                saved.getParentCommentId(),
                List.of(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }

        comment.softDelete();
        commentRepository.update(comment);

        postRepository.decrementCommentCount(comment.getPostId());
    }
}
