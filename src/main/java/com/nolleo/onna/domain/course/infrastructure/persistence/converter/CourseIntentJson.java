package com.nolleo.onna.domain.course.infrastructure.persistence.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;

/**
 * CourseIntent ↔ JSON 문자열 변환 유틸.
 * 엔티티의 intent(JSONB) 컬럼 저장/복원에 사용한다.
 */
public final class CourseIntentJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CourseIntentJson() {}

    public static String toJson(CourseIntent intent) {
        if (intent == null) return null;
        try {
            return MAPPER.writeValueAsString(intent);
        } catch (Exception e) {
            throw new IllegalStateException("CourseIntent 직렬화 실패", e);
        }
    }

    public static CourseIntent fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, CourseIntent.class);
        } catch (Exception e) {
            throw new IllegalStateException("CourseIntent 역직렬화 실패", e);
        }
    }
}
