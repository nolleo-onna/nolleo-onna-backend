package com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.repository;

import com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.entity.GeneratedCourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedCourseJpaRepository extends JpaRepository<GeneratedCourseEntity, Long> {

    List<GeneratedCourseEntity> findByPairId(UUID pairId);
}
