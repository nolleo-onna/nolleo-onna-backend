package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {

    @EntityGraph(attributePaths = "items")
    List<CourseEntity> findByPairId(UUID pairId);

    @EntityGraph(attributePaths = "items")
    List<CourseEntity> findByUserId(Long userId);
}
