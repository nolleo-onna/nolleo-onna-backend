package com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.repository;

import com.nolleo.onna.domain.generatedcourse.domain.repository.GeneratedCourseItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GeneratedCourseItemRepositoryImpl implements GeneratedCourseItemRepository {

    private final GeneratedCourseItemJpaRepository jpaRepository;

}
