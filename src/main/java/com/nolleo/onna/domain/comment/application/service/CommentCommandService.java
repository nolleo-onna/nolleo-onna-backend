package com.nolleo.onna.domain.comment.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.comment.application.dto.CommentResult;
import com.nolleo.onna.domain.comment.application.dto.CreateCommentCommand;
import com.nolleo.onna.domain.comment.domain.exception.CommentErrorCode;
import com.nolleo.onna.domain.comment.domain.model.Comment;
import com.nolleo.onna.domain.comment.domain.repository.CommentRepository;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
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
    private final UserLookupPort userLookupPort;

    public CommentResult createComment(Long userId, CreateCommentCommand command) {
        postRepository.findById(command.postId())
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (command.parentCommentId() != null) {
            Comment parent = commentRepository.findById(command.parentCommentId())
                    .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

            if (parent.getParentCommentId() != null) {
                throw new BusinessException(CommentErrorCode.INVALID_PARENT_COMMENT);
            }
            if (!parent.getPostId().equals(command.postId())) {
                throw new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND);
            }
        }

        Comment comment = Comment.create(command.postId(), userId, command.parentCommentId(), command.content());
        Comment saved = commentRepository.save(comment);

        postRepository.incrementCommentCount(command.postId());

        UserLookupPort.UserProfile profile = userLookupPort.findById(userId).orElse(null);
        String nickname = profile != null ? profile.nickname() : "알 수 없음";
        String profileImageUrl = profile != null ? profile.profileImageUrl() : null;

        return new CommentResult(
                saved.getId(),
                nickname,
                profileImageUrl,
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
