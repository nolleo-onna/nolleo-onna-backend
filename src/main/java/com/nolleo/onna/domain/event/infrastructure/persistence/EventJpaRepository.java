package com.nolleo.onna.domain.event.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventJpaRepository extends JpaRepository<EventEntity, String> {

    @Query("SELECT e FROM EventEntity e WHERE e.active = true")
    List<EventEntity> findAllActive();

    Optional<EventEntity> findByContentIdAndActiveTrue(String contentId);

    /**
     * 활성 행사 중 제목이 키워드를 포함하고 today 기준 아직 끝나지 않은 행사를 "정확히 일치 → 시작일 이른 순"으로 limit개 조회.
     * 비교는 양쪽 공백을 제거하고 대소문자 무시로 한다. compactKeyword는 호출자가 공백을 제거하고 LIKE 와일드카드를 이스케이프해서 넘긴다.
     * 코스 기준점("X 근처") 매칭용 — 끝난 행사는 기준점이 될 수 없다.
     */
    @Query(value = """
            SELECT * FROM ev_events
            WHERE is_active = true
              AND REPLACE(title, ' ', '') ILIKE CONCAT('%', :compactKeyword, '%')
              AND (event_end_date IS NULL OR event_end_date >= :today)
            ORDER BY (LOWER(REPLACE(title, ' ', '')) = LOWER(:compactKeyword)) DESC,
                     event_start_date ASC NULLS LAST
            LIMIT :limit
            """, nativeQuery = true)
    List<EventEntity> findActiveByTitleNotEnded(@Param("compactKeyword") String compactKeyword,
                                                @Param("today") LocalDate today,
                                                @Param("limit") int limit);
}
