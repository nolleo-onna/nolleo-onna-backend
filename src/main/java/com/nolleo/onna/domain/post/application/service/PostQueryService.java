package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.application.service.ViewCountRecorder;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.post.application.dto.PostDetailResult;
import com.nolleo.onna.domain.post.application.dto.PostPopularResult;
import com.nolleo.onna.domain.post.application.dto.PostSummaryResult;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.repository.PostLikeRepository;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.domain.repository.PostSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserLookupPort userLookupPort;
    private final ViewCountRecorder viewCountRecorder;

    /**
     * 게시글 상세 조회. 조회는 조회수 버퍼(Redis)에 기록되고(같은 viewer는 10분에 1회만 집계),
     * 응답 조회수는 DB 값 + 아직 DB에 반영되지 않은 대기분(이번 조회 포함)이다.
     * 버퍼 장애 시 DB에 바로 +1 하므로(@Modifying) readOnly 트랜잭션이 아니다.
     *
     * 조회 기록은 다른 조회가 모두 성공한 뒤 마지막에 한다 — 뒤 단계가 실패해 롤백돼도 되돌릴 수 없는 Redis 집계만 남는 일을 막는다.
     * Redis 호출이 트랜잭션(DB 커넥션 점유) 안에서 일어나므로 지연 상한은 spring.data.redis.timeout으로 제한한다.
     *
     * @param viewerKey 조회자 식별 키 (ViewerKeyResolver)
     */
    @Transactional
    public PostDetailResult getPost(Long postId, Long userId, String viewerKey) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        boolean isLiked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);

        UserLookupPort.UserProfile profile = userLookupPort.findById(post.getUserId()).orElse(null);
        String nickname = profile != null ? profile.nickname() : "알 수 없음";
        String profileImageUrl = profile != null ? profile.profileImageUrl() : null;

        long pendingViews = viewCountRecorder.record(PostViewCountSink.TARGET_TYPE, postId, viewerKey,
                () -> postRepository.incrementViewCount(postId));
        post.applyPendingViews(pendingViews);

        return new PostDetailResult(post, nickname, profileImageUrl, isLiked);
    }

    public Page<PostSummaryResult> getPosts(PostSearchCondition condition, Pageable pageable, Long userId) {
        Page<Post> posts = postRepository.findAll(condition, pageable);

        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();
        Set<Long> likedPostIds = userId != null
                ? postLikeRepository.findLikedPostIds(userId, postIds)
                : Set.of();

        // 작성자 N+1 방지: 고유 userId 목록으로 일괄 조회
        List<Long> authorIds = posts.getContent().stream().map(Post::getUserId).distinct().toList();
        Map<Long, UserLookupPort.UserProfile> profileMap = authorIds.stream()
                .map(id -> Map.entry(id, userLookupPort.findById(id)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        return posts.map(post -> {
            UserLookupPort.UserProfile profile = profileMap.get(post.getUserId());
            String nickname = profile != null ? profile.nickname() : "알 수 없음";
            String profileImageUrl = profile != null ? profile.profileImageUrl() : null;
            return new PostSummaryResult(post, nickname, profileImageUrl, likedPostIds.contains(post.getId()));
        });
    }

    public List<PostPopularResult> getPopularPosts(Long userId) {
        List<Post> posts = postRepository.findPopular(5);

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Set<Long> likedPostIds = userId != null
                ? postLikeRepository.findLikedPostIds(userId, postIds)
                : Set.of();

        return posts.stream().map(post -> {
            String thumbnail = (post.getImageUrls() != null && !post.getImageUrls().isEmpty())
                    ? post.getImageUrls().get(0)
                    : null;
            return new PostPopularResult(post.getId(), post.getTitle(), thumbnail, post.getLikeCount(), likedPostIds.contains(post.getId()));
        }).toList();
    }
}
