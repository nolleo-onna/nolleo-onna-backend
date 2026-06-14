package com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.repository;

import com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.entity.GeneratedCourseItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedCourseItemJpaRepository extends JpaRepository<GeneratedCourseItemEntity, Long> {

}
