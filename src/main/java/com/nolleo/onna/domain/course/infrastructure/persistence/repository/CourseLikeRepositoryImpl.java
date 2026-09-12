package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.domain.repository.CourseLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseLikeRepositoryImpl implements CourseLikeRepository {

    private final CourseLikeJpaRepository jpaRepository;

    @Override
    public boolean add(Long courseId, Long userId) {
        return jpaRepository.insertIfAbsent(courseId, userId) == 1;
    }

    @Override
    public boolean remove(Long courseId, Long userId) {
        return jpaRepository.deleteByCourseIdAndUserId(courseId, userId) == 1;
    }

    @Override
    public boolean exists(Long courseId, Long userId) {
        return jpaRepository.existsByCourseIdAndUserId(courseId, userId);
    }
}
