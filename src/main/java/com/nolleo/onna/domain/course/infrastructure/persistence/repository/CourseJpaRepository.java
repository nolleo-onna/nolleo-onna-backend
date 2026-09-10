package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
