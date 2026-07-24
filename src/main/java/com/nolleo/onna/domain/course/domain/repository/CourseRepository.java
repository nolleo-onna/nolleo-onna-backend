package com.nolleo.onna.domain.course.domain.repository;

import com.nolleo.onna.domain.course.domain.model.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(Long id);

    List<Course> findByPairId(UUID pairId);

    List<Course> findByUserId(Long userId);
}
