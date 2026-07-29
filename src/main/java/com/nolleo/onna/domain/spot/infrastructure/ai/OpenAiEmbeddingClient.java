package com.nolleo.onna.domain.spot.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.spot.application.port.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EmbeddingClient 포트의 OpenAI Embeddings API 구현.
 * spot_embeddings 테이블과 동일 모델(text-embedding-3-small, 1536차원)을 사용해야
 * 벡터 공간이 일치한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final String URL = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.embedding-model}")
    private String model;

    /** 텍스트를 임베딩 벡터(1536차원)로 변환 */
    @Override
    public List<Float> embed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", text
        );

        try {
            String response = restTemplate.postForObject(URL, new HttpEntity<>(body, headers), String.class);
            return extractEmbedding(response);
        } catch (HttpStatusCodeException e) {
            log.error("OpenAI Embeddings API HTTP {} 오류 | 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OpenAiApiException("OpenAI Embeddings API 오류: " + e.getStatusCode(), e);
        }
    }

    private List<Float> extractEmbedding(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                log.error("OpenAI 응답 형식 이상: {}", response);
                throw new OpenAiApiException("OpenAI 응답에서 임베딩을 찾을 수 없습니다.");
            }
            List<Float> embedding = new ArrayList<>(embeddingNode.size());
            embeddingNode.forEach(node -> embedding.add((float) node.asDouble()));
            return embedding;
        } catch (OpenAiApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenAiApiException("OpenAI 응답 파싱 실패", e);
        }
    }

    public static class OpenAiApiException extends RuntimeException {
        public OpenAiApiException(String message) { super(message); }
        public OpenAiApiException(String message, Throwable cause) { super(message, cause); }
    }
}
