package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
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

    @Transactional
    public PostDetailResult getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        postRepository.incrementViewCount(postId);

        boolean isLiked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);

        UserLookupPort.UserProfile profile = userLookupPort.findById(post.getUserId()).orElse(null);
        String nickname = profile != null ? profile.nickname() : "알 수 없음";
        String profileImageUrl = profile != null ? profile.profileImageUrl() : null;

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
