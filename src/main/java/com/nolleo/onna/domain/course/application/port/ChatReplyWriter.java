package com.nolleo.onna.domain.course.application.port;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

/**
 * [아웃바운드 포트] 챗봇 응답 문구 생성.
 * 구현은 infrastructure/ai에 위치한다.
 */
public interface ChatReplyWriter {

    /** 여행 코스 요청과 무관한 메시지일 때 — 서비스 안내 후 리다이렉트 */
    String offTopic();

    /** 시작 지역이 없을 때 — 지역 되묻기 */
    String askStartArea();

    /** 선택 필드(예산·동행·분위기)가 전부 없을 때 — 묶어서 1회 되묻기 */
    String askPreferences(CourseIntent intent);

    /** 필수·선택 정보가 모두 모였을 때 — 지금까지 파악한 조건으로 생성해도 될지 확인 */
    String confirmGenerate(CourseIntent intent);

    /** 생성 완료 안내 */
    String ready(CourseIntent intent);
}
