package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.application.port.ViewCountSink;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 코스 공유 링크 조회수를 DB(generated_courses.view_count)에 일괄 반영하는 조회수 동기화 대상 */
@Service
@RequiredArgsConstructor
public class CourseViewCountSink implements ViewCountSink {

    /** 조회수 버퍼의 코스 대상 타입 — CourseShareService.getShared의 기록과 같은 값이어야 한다 */
    public static final String TARGET_TYPE = "course";

    private final CourseRepository courseRepository;

    @Override
    public String targetType() {
        return TARGET_TYPE;
    }

    @Override
    @Transactional
    public void addViewCounts(Map<Long, Long> deltaByTargetId) {
        courseRepository.addViewCounts(deltaByTargetId);
    }
}
