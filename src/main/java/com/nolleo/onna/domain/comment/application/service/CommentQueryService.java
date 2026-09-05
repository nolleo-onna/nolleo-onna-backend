package com.nolleo.onna.domain.comment.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.domain.comment.application.dto.CommentResult;
import com.nolleo.onna.domain.comment.domain.model.Comment;
import com.nolleo.onna.domain.comment.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final UserLookupPort userLookupPort;

    public Page<CommentResult> getComments(Long postId, Pageable pageable) {
        Page<Comment> topLevelComments = commentRepository.findTopLevelByPostId(postId, pageable);

        List<Long> parentIds = topLevelComments.getContent().stream()
                .map(Comment::getId)
                .toList();

        List<Comment> replies = commentRepository.findRepliesByParentIds(parentIds);

        Map<Long, List<Comment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentCommentId));

        // 작성자 N+1 방지: 최상위 댓글 + 대댓글 작성자 일괄 조회
        List<Long> allUserIds = Stream.concat(
                topLevelComments.getContent().stream().map(Comment::getUserId),
                replies.stream().map(Comment::getUserId)
        ).distinct().toList();

        Map<Long, UserLookupPort.UserProfile> profileMap = allUserIds.stream()
                .map(id -> Map.entry(id, userLookupPort.findById(id)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        return topLevelComments.map(comment ->
                toResult(comment, repliesByParentId.getOrDefault(comment.getId(), List.of()), profileMap));
    }

    private CommentResult toResult(Comment comment, List<Comment> replies, Map<Long, UserLookupPort.UserProfile> profileMap) {
        UserLookupPort.UserProfile profile = profileMap.get(comment.getUserId());
        String nickname = profile != null ? profile.nickname() : "알 수 없음";
        String profileImageUrl = profile != null ? profile.profileImageUrl() : null;

        List<CommentResult> replyResults = replies.stream()
                .map(reply -> toResult(reply, List.of(), profileMap))
                .toList();

        return new CommentResult(
                comment.getId(),
                nickname,
                profileImageUrl,
                comment.getContent(),
                comment.isDeleted(),
                comment.getParentCommentId(),
                replyResults,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
