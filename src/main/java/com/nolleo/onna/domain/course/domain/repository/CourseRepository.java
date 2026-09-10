package com.nolleo.onna.domain.course.domain.repository;

import com.nolleo.onna.domain.course.domain.model.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {

    Course save(Course course);

    /**
     * 아이템 목록이 교체된 코스를 반영한다 (코스 수정 = 최종 리스트 일괄 반영).
     * 기존 아이템 행은 전부 삭제되고 새 순번으로 다시 삽입된다.
     */
    Course update(Course course, String updatedBy);

    Optional<Course> findById(Long id);

    List<Course> findByPairId(UUID pairId);

    List<Course> findByUserId(Long userId);
}
