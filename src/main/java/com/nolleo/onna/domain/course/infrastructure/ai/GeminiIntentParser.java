package com.nolleo.onna.domain.course.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.course.application.dto.ParsedMessage;
import com.nolleo.onna.domain.course.application.port.CourseIntentParser;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.SlotHints;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CourseIntentParser의 Gemini 구현.
 *
 * 규칙:
 *   - 언급되지 않은 필드는 null (AI가 추측해서 채우지 않음)
 *   - startArea는 DistrictCenter 지원 지역명으로 정규화 (랜드마크 → 지역 변환 포함)
 *   - 매칭 실패한 지역은 null 처리 → 상위에서 되묻기로 이어짐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiIntentParser implements CourseIntentParser {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_INSTRUCTION = """
            너는 부산 여행 코스 생성 서비스의 자연어 파싱 엔진이다.
            사용자의 메시지에서 여행 의도를 추출해 JSON으로만 응답한다.

            ## 출력 스키마
            {
              "isTravelRelated": boolean,        // 부산 여행 코스 추천 요청과 관련 있는 메시지인지.
                                                  // 지역/음식/관광/동행/분위기/예산 등 여행 코스 구성에 관한 내용이면 true.
                                                  // 그 외 잡담, 다른 주제 질문, 서비스와 무관한 요청이면 false.
              "startArea": string | null,       // 시작 지역. 반드시 지원 지역 목록 중 하나로 변환. 매칭 불가면 null
              "nearbyAllowed": boolean,          // "근처", "근방", "주변도 괜찮아" 언급 시 true, 아니면 false
              "budget": number | null,           // 총 예산(원). "5만원" → 50000. 언급 없으면 null
              "companion": string | null,        // 동행 유형: 연인/친구/가족/아이/혼자 중 하나. 언급 없으면 null
              "mood": string[],                  // 분위기 태그: 감성적인/로맨틱한/아늑한/활기찬/차분한/고즈넉한 등. 언급 없으면 []
              "slotHints": {
                "foodCount": number | null,      // 식사 횟수. "점심 저녁" → 2. 언급 없으면 null
                "cafeCount": number | null,      // 카페 방문 횟수
                "attractionCount": number | null,// 관광지 방문 횟수. "관광지 한 군데" → 1
                "activityCount": number | null   // 체험/레저 횟수
              }
            }

            ## 지원 지역 목록 (startArea는 반드시 이 중 하나)
            %s

            ## 규칙
            1. 언급되지 않은 필드는 반드시 null (추측 금지)
            2. 랜드마크가 언급되면 해당 지역으로 변환 (예: "흰여울문화마을" → "영도", "광안대교" → "광안리", "해리단길" → "해운대", "감천문화마을" → "사하구", "벡스코" → "센텀")
            3. 부산이 아닌 지역이 언급되면 startArea는 null
            4. "먹거리 여행", "맛집 투어" 같은 테마 표현은 foodCount를 2 이상으로 해석해도 됨 (명시적 횟수가 없으면 2)
            5. 날짜, 요일 등 시간 정보는 무시
            6. isTravelRelated=false인 경우 다른 필드는 전부 null/false/[]로 채운다
            7. JSON 외 다른 텍스트 출력 금지
            """;

    @Override
    public ParsedMessage parse(String message) {
        String supportedAreas = Arrays.stream(DistrictCenter.values())
                .map(DistrictCenter::getSigngu)
                .collect(Collectors.joining(", "));

        String json = geminiClient.generateJson(
                String.format(SYSTEM_INSTRUCTION, supportedAreas), message);

        try {
            JsonNode node = objectMapper.readTree(json);
            boolean isTravelRelated = node.path("isTravelRelated").asBoolean(true);
            if (!isTravelRelated) {
                return new ParsedMessage(false, CourseIntent.empty());
            }
            return new ParsedMessage(true, toIntent(node));
        } catch (Exception e) {
            log.error("Intent 파싱 실패 | Gemini 응답: {}", json, e);
            throw new GeminiClient.GeminiApiException("의도 분석 결과 파싱 실패", e);
        }
    }

    private CourseIntent toIntent(JsonNode node) {
        String startArea = normalizeArea(textOrNull(node, "startArea"));

        JsonNode hints = node.path("slotHints");
        SlotHints slotHints = new SlotHints(
                intOrNull(hints, "foodCount"),
                intOrNull(hints, "cafeCount"),
                intOrNull(hints, "attractionCount"),
                intOrNull(hints, "activityCount")
        );

        List<String> mood = new ArrayList<>();
        if (node.path("mood").isArray()) {
            node.path("mood").forEach(m -> mood.add(m.asText()));
        }

        return new CourseIntent(
                startArea,
                node.path("nearbyAllowed").asBoolean(false),
                intOrNull(node, "budget"),
                textOrNull(node, "companion"),
                mood,
                slotHints,
                false
        );
    }

    /** AI가 반환한 지역명을 DistrictCenter와 재검증 — 미지원 지역은 null */
    private String normalizeArea(String area) {
        if (area == null) return null;
        return DistrictCenter.of(area).map(DistrictCenter::getSigngu).orElse(null);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isTextual() && !v.asText().isBlank() ? v.asText() : null;
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.asInt() : null;
    }
}
