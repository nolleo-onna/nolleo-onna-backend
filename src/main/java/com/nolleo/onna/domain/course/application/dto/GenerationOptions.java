package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;

/**
 * 코스 생성 파이프라인의 경로별 옵션 — 챗봇(AI)과 폼(ALGORITHM)이 같은 파이프라인을 쓰되 외부 호출 여부가 다르다.
 *
 *   createdBy      감사 컬럼에 남길 생성 주체 ("AI_CHAT" / "FORM")
 *   useRerank      무드·동행 기반 벡터 리랭킹(OpenAI 임베딩) 사용 여부. 폼은 동행·무드를 받지 않으므로 끈다
 *   templateTitle  제목 템플릿. null이면 조립 후 AI(CourseContentWriter)가 제목·소개를 만든다
 */
public record GenerationOptions(GenerationMode mode, String createdBy, boolean useRerank, String templateTitle) {

    /** 챗봇 — 리랭킹 사용, 제목·소개는 Gemini */
    public static GenerationOptions aiChat() {
        return new GenerationOptions(GenerationMode.AI, "AI_CHAT", true, null);
    }

    /** 폼 — 외부 AI 호출 없음. 제목은 템플릿, 소개는 비움 */
    public static GenerationOptions form(String templateTitle) {
        return new GenerationOptions(GenerationMode.ALGORITHM, "FORM", false, templateTitle);
    }

    public boolean usesAiContent() {
        return templateTitle == null;
    }
}
