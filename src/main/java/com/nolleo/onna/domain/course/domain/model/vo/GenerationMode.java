package com.nolleo.onna.domain.course.domain.model.vo;

/** 코스 생성 방식 */
public enum GenerationMode {
    AI,         // 자연어 챗봇 (Gemini 파싱 + 콘텐츠 생성)
    ALGORITHM   // 폼 입력 기반 알고리즘 (AI 하루 제한 소진 시 포함)
}
