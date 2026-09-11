package com.nolleo.onna.domain.post.domain.repository;

import com.nolleo.onna.domain.post.domain.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
    Page<Post> findAll(PostSearchCondition condition, Pageable pageable);
    List<Post> findPopular(int limit);
    void incrementLikeCount(Long postId);
    void decrementLikeCount(Long postId);
    void incrementCommentCount(Long postId);
    void decrementCommentCount(Long postId);
    /** 조회수 +1 (원자 UPDATE) — 조회수 버퍼(Redis)를 쓸 수 없을 때의 대체 경로 */
    void incrementViewCount(Long postId);
    /** 게시글 id별 조회수를 더한다 (view_count = view_count + delta) — 조회수 버퍼 동기화용, 호출자 트랜잭션 안에서 실행된다 */
    void addViewCounts(Map<Long, Long> deltaByPostId);
    void softDelete(Long postId, String deletedBy);
    Post update(Long postId, Post post);
}
