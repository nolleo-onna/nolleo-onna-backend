package com.nolleo.onna.domain.course.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.course.application.port.CourseContentWriter;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CourseContentWriter의 Gemini 구현.
 * 코스 조립이 끝난 뒤(방문 순서 확정 후) 제목·소개 문구를 생성한다.
 * Gemini 호출 실패 시 지역명 기반 템플릿으로 폴백 — 코스 저장이 실패하지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiCourseContentWriter implements CourseContentWriter {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_INSTRUCTION = """
            너는 부산 여행 코스 추천 챗봇이다. 완성된 방문 순서를 보고
            코스 제목과 한 줄 소개를 생성한다.
            반드시 JSON으로만 답한다: {"title": "...", "description": "..."}
            title은 15자 이내, description은 40자 이내 한국어 존댓말 한 문장.
            """;

    @Override
    public CourseContent generate(CourseIntent intent, List<String> spotTitlesInOrder) {
        try {
            String userMessage = String.format(
                    "시작 지역: %s, 방문 순서: %s", intent.startArea(), String.join(" → ", spotTitlesInOrder));
            String json = geminiClient.generateJson(SYSTEM_INSTRUCTION, userMessage);
            JsonNode root = objectMapper.readTree(json);
            String title = root.path("title").asText(null);
            String description = root.path("description").asText(null);
            if (title == null || title.isBlank()) {
                throw new IllegalStateException("title이 비어 있습니다.");
            }
            return new CourseContent(title, description);
        } catch (Exception e) {
            log.warn("코스 제목 생성 실패 — 기본 템플릿 사용: {}", e.getMessage());
            return new CourseContent(intent.startArea() + " 코스", intent.startArea() + " 여행 코스를 준비했어요.");
        }
    }
}
