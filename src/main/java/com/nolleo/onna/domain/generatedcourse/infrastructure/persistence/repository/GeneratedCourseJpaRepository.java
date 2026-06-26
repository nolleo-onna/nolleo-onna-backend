package com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.repository;

import com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.entity.GeneratedCourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedCourseJpaRepository extends JpaRepository<GeneratedCourseEntity, Long> {

}
