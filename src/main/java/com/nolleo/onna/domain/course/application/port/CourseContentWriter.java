package com.nolleo.onna.domain.course.application.port;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

import java.util.List;

/**
 * [아웃바운드 포트] 코스 구성 완료 후 제목·소개 문구 생성.
 * 구현은 infrastructure/ai에 위치한다.
 */
public interface CourseContentWriter {

    record CourseContent(String title, String description) {
    }

    /** 코스가 완전히 조립된 뒤(방문 순서 확정 후) 호출한다. */
    CourseContent generate(CourseIntent intent, List<String> spotTitlesInOrder);
}
