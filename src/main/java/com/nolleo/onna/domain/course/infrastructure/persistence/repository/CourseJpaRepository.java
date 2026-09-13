package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {

    /** 단건 조회 + 아이템 fetch join — 코스와 아이템을 한 쿼리로 가져온다 */
    @EntityGraph(attributePaths = "items")
    Optional<CourseEntity> findWithItemsById(Long id);

    /**
     * 단건 조회 + 행 잠금(PESSIMISTIC_WRITE) — 공유 상태 전환처럼 읽고 판단해 쓰는 경로용.
     * 아이템을 fetch join하지 않는다: PostgreSQL은 outer join의 nullable 쪽에 FOR UPDATE를 허용하지 않는다.
     * 아이템은 같은 트랜잭션 안에서 지연 로딩된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CourseEntity c WHERE c.id = :id")
    Optional<CourseEntity> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "items")
    List<CourseEntity> findByPairId(UUID pairId);

    @EntityGraph(attributePaths = "items")
    List<CourseEntity> findByUserId(Long userId);

    /** 공유 토큰으로 공개 코스 조회 + 아이템 fetch — 비공개·소프트 삭제된 코스는 제외 */
    @EntityGraph(attributePaths = "items")
    @Query("""
            SELECT c FROM CourseEntity c
             WHERE c.shareToken = :shareToken
               AND c.isPublic = true
               AND c.softDeleteAudit.deletedAt IS NULL
            """)
    Optional<CourseEntity> findPublicByShareToken(@Param("shareToken") String shareToken);

    /**
     * 공개 코스 id 페이지 — 인기순(조회수 내림차순 → 최신순 → id 내림차순으로 순서 고정).
     * 아이템 fetch join과 LIMIT을 한 쿼리에 섞으면 Hibernate가 전체를 메모리로 읽어 자르므로(HHH90003004),
     * id만 페이징한 뒤 findWithItemsByIdIn 으로 본문을 가져오는 2단계로 나눈다.
     */
    @Query("""
            SELECT c.id FROM CourseEntity c
             WHERE c.isPublic = true
               AND c.softDeleteAudit.deletedAt IS NULL
             ORDER BY c.viewCount DESC, c.createAudit.createdAt DESC, c.id DESC
            """)
    List<Long> findPublicIdsOrderByPopularity(Pageable pageable);

    /** id 목록으로 코스 + 아이템 일괄 조회 — 순서는 보장하지 않으므로 호출자가 id 순서대로 다시 정렬한다 */
    @EntityGraph(attributePaths = "items")
    List<CourseEntity> findWithItemsByIdIn(Collection<Long> ids);

    /** 조회수 원자 증가 — PostJpaRepository.incrementLikeCount 와 동일 패턴 */
    @Modifying
    @Query("UPDATE CourseEntity c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /** 좋아요 수 원자 증가 — PostJpaRepository.incrementLikeCount 와 동일 패턴 */
    @Modifying
    @Query("UPDATE CourseEntity c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    /** 좋아요 수 원자 감소 — 0 미만으로 내려가지 않는다 (DB CHECK like_count >= 0 과 이중 방어) */
    @Modifying
    @Query("UPDATE CourseEntity c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    /** 좋아요 수 스칼라 조회 — 벌크 UPDATE 직후 1차 캐시의 stale 엔티티를 거치지 않고 DB 값을 읽는다 */
    @Query("SELECT c.likeCount FROM CourseEntity c WHERE c.id = :id")
    Integer findLikeCount(@Param("id") Long id);

    /** 조회수 일괄 가산 — 조회수 버퍼 동기화에서 코스별로 호출한다 */
    @Modifying
    @Query("UPDATE CourseEntity c SET c.viewCount = c.viewCount + :delta WHERE c.id = :id")
    void addViewCount(@Param("id") Long id, @Param("delta") int delta);
}
