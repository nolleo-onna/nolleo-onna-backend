package com.nolleo.onna.domain.course.application.port;

import com.nolleo.onna.domain.course.application.dto.ParsedMessage;

/**
 * [아웃바운드 포트] 자연어 → CourseIntent 변환.
 * 구현은 infrastructure/ai에 위치하며, application 레이어는 어떤 LLM을 쓰는지 알지 못한다.
 */
public interface CourseIntentParser {

    /**
     * 사용자 자연어 메시지에서 코스 생성 의도를 추출한다.
     * 여행 코스 요청과 무관한 메시지면 isTravelRelated=false로 반환한다.
     * 관련 있는 경우 언급되지 않은 필드는 null/empty로 채워진 부분 intent를 반환한다.
     */
    ParsedMessage parse(String message);
}
