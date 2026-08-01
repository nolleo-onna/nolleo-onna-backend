package com.nolleo.onna.domain.course.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Gemini generateContent API 공통 클라이언트.
 * generateJson: responseMimeType=application/json 강제 → 항상 JSON 텍스트 반환
 * generateText: 일반 텍스트 (되묻기 질문, 코스 소개 문구)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final String URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.model}")
    private String model;

    /** JSON 응답 생성 — 파싱은 호출자 책임 */
    public String generateJson(String systemInstruction, String userMessage) {
        return call(buildBody(systemInstruction, userMessage, Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.2
        )));
    }

    /** 일반 텍스트 생성 */
    public String generateText(String systemInstruction, String userMessage) {
        return call(buildBody(systemInstruction, userMessage, Map.of("temperature", 0.7)));
    }

    private Map<String, Object> buildBody(String systemInstruction, String userMessage,
                                           Map<String, Object> generationConfig) {
        return Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userMessage))
                )),
                "generationConfig", generationConfig
        );
    }

    private String call(Map<String, Object> body) {
        String url = String.format(URL_TEMPLATE, model, apiKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            return extractText(response);
        } catch (HttpStatusCodeException e) {
            log.error("Gemini API HTTP {} 오류 | 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeminiApiException("Gemini API 오류: " + e.getStatusCode(), e);
        }
    }

    /** candidates[0].content.parts[0].text 추출 */
    private String extractText(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                log.error("Gemini 응답 형식 이상: {}", response);
                throw new GeminiApiException("Gemini 응답에서 텍스트를 찾을 수 없습니다.");
            }
            return text.asText();
        } catch (GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiApiException("Gemini 응답 파싱 실패", e);
        }
    }

    public static class GeminiApiException extends RuntimeException {
        public GeminiApiException(String message) { super(message); }
        public GeminiApiException(String message, Throwable cause) { super(message, cause); }
    }
}
