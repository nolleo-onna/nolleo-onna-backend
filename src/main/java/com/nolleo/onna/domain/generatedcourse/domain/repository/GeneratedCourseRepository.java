package com.nolleo.onna.domain.generatedcourse.domain.repository;

import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourse;

import java.util.List;
import java.util.UUID;

public interface GeneratedCourseRepository {

    /** 코스 저장 (신규 생성) */
    GeneratedCourse save(GeneratedCourse course);

    /** pairId에 속한 코스 목록 (ACTIVE·CULTURE·FOOD_TOUR 3개) */
    List<GeneratedCourse> findByPairId(UUID pairId);
}
