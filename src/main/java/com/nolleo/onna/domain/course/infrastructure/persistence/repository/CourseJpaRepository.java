package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {

    /** 단건 조회 + 아이템 fetch join — 코스와 아이템을 한 쿼리로 가져온다 */
    @EntityGraph(attributePaths = "items")
    Optional<CourseEntity> findWithItemsById(Long id);

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

    /** 조회수 원자 증가 — PostJpaRepository.incrementLikeCount 와 동일 패턴 */
    @Modifying
    @Query("UPDATE CourseEntity c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
