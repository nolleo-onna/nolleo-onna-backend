package com.nolleo.onna.domain.course.domain.repository;

import com.nolleo.onna.domain.course.domain.model.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {

    Course save(Course course);

    /**
     * 방문 스팟 목록과 그에 따른 totalCost만 반영한다 (코스 수정 = 최종 리스트 일괄 반영).
     * 기존 아이템 행은 전부 삭제되고 새 순번으로 다시 삽입된다.
     * 제목·소개·공유 상태 등 다른 필드의 변경은 반영하지 않는다 — 필요해지면 별도 메서드로 연다.
     *
     * @param actor 변경 주체. updated_by와, 새로 삽입되는 아이템 행의 created_by에 기록된다
     */
    Course saveReplacedItems(Course course, String actor);

    Optional<Course> findById(Long id);

    List<Course> findByPairId(UUID pairId);

    List<Course> findByUserId(Long userId);
}
