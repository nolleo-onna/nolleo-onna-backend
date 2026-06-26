package com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.repository;

import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourse;
import com.nolleo.onna.domain.generatedcourse.domain.repository.GeneratedCourseRepository;
import com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.entity.GeneratedCourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GeneratedCourseRepositoryImpl implements GeneratedCourseRepository {

    private final GeneratedCourseJpaRepository jpaRepository;

}
