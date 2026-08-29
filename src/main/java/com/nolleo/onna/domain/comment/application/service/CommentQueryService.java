package com.nolleo.onna.domain.comment.application.service;

import com.nolleo.onna.domain.comment.domain.model.Comment;
import com.nolleo.onna.domain.comment.domain.repository.CommentRepository;
import com.nolleo.onna.domain.comment.presentation.dto.response.CommentResponse;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final UserJpaRepository userJpaRepository;

    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        Page<Comment> topLevelComments = commentRepository.findTopLevelByPostId(postId, pageable);

        List<Long> parentIds = topLevelComments.getContent().stream()
                .map(Comment::getId)
                .toList();

        List<Comment> replies = commentRepository.findRepliesByParentIds(parentIds);

        Map<Long, List<Comment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentCommentId));

        return topLevelComments.map(comment -> toResponse(comment, repliesByParentId.getOrDefault(comment.getId(), List.of())));
    }

    private CommentResponse toResponse(Comment comment, List<Comment> replies) {
        UserEntity user = userJpaRepository.findById(comment.getUserId()).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";
        String profileImageUrl = user != null ? user.getProfileImageUrl() : null;

        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> toResponse(reply, List.of()))
                .toList();

        return new CommentResponse(
                comment.getId(),
                new CommentResponse.AuthorInfo(nickname, profileImageUrl),
                comment.getContent(),
                comment.isDeleted(),
                comment.getParentCommentId(),
                replyResponses,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
