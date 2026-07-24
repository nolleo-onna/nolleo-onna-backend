package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseJpaRepository jpaRepository;

    @Override
    public Course save(Course course) {
        return jpaRepository.save(CourseEntity.fromDomain(course)).toDomain();
    }

    @Override
    public Optional<Course> findById(Long id) {
        return jpaRepository.findById(id).map(CourseEntity::toDomain);
    }

    @Override
    public List<Course> findByPairId(UUID pairId) {
        return jpaRepository.findByPairId(pairId).stream()
                .map(CourseEntity::toDomain)
                .toList();
    }

    @Override
    public List<Course> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(CourseEntity::toDomain)
                .toList();
    }
}
