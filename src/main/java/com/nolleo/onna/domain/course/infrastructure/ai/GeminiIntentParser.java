package com.nolleo.onna.domain.course.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.ParsedMessage;
import com.nolleo.onna.domain.course.application.port.CourseIntentParser;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.SlotHints;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
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
              },
              "includeSpots": string[],          // 코스에 꼭 넣어달라고 한 구체적 장소명 원문. "광안리 바다는 꼭 넣어줘" → ["광안리 바다"]. 언급 없으면 []
              "excludeSpots": string[],          // 코스에서 빼달라고 한 구체적 장소명 원문. "해운대 해수욕장은 빼줘" → ["해운대 해수욕장"]. 언급 없으면 []
              "spotNameHints": { "<원문>": "<공식 명칭>" },  // includeSpots/excludeSpots 원문별 한국관광공사 공식 명칭 추측. 확실한 것만. 없으면 {}
              "anchor": string | null            // "X 근처/주변/가는 김에"의 X — 코스의 기준이 되는 행사·장소명 원문. 지역명만 말했으면 null
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
            7. includeSpots/excludeSpots에는 사용자가 말한 구체적 장소(해수욕장·공원·시장·상호 등)를 **원문 그대로** 담는다 — 바꾸지 않는다.
               "광안리", "해운대"처럼 지역명만 말한 경우는 startArea로만 쓰고 여기에 넣지 않는다.
               "넣어줘/포함/들르고 싶어/꼭 가야 해"는 includeSpots, "빼줘/제외/가기 싫어/말고"는 excludeSpots.
               포함 장소가 있고 지역 언급이 없으면 그 장소의 지역을 startArea로 써도 된다 (예: "광안리 해수욕장 넣어줘" → startArea "광안리")
            8. spotNameHints에는 원문이 구어체·별칭일 때 한국관광공사 공식 명칭 추측을 원문을 키로 담는다 — 참고용이며 실제 판단은 데이터가 한다.
               예: {"광안리 바다": "광안리해수욕장", "자갈치": "자갈치시장", "감천마을": "감천문화마을"}. 확실하지 않으면 넣지 않는다 (지어내지 않는다)
            9. 행사·장소를 기준으로 "근처/주변/가까운/가는 김에/보고 나서"라고 하거나, 행사를 묻는 질문("X 행사 있어?", "X 관련 행사 뭐 있어?",
               "X 축제 언제야?")이면 그 이름을 anchor에 담는다. 이름만 담고 "행사·축제·페스티벌·관련·근처" 같은 일반어는 뗀다.
               (예: "부산국제항만컨퍼런스 근처 갈만한 곳" → "부산국제항만컨퍼런스", "부산불꽃축제 행사 축제 갈껀데 근처 코스" → "부산불꽃축제",
                "부산국제 관련 행사 있지 않아?" → "부산국제")
               anchor의 위치를 추측해서 startArea를 채우지 마라 — 위치는 데이터에서 찾는다. startArea는 사용자가 지역명을 직접 말했을 때만 채운다.
               "광안리 근처"처럼 지역명이면 anchor가 아니라 startArea다.
            10. JSON 외 다른 텍스트 출력 금지
            """;

    /**
     * 의도 파싱은 폴백할 수 없는 단계다(파싱이 안 되면 대화를 진행할 수 없다).
     * Gemini 호출·응답 해석 실패는 AI_SERVICE_UNAVAILABLE(503)로 변환해 500과 내부 메시지가 노출되지 않게 한다.
     */
    @Override
    public ParsedMessage parse(String message) {
        String supportedAreas = Arrays.stream(DistrictCenter.values())
                .map(DistrictCenter::getSigngu)
                .collect(Collectors.joining(", "));

        String json;
        try {
            json = geminiClient.generateJson(String.format(SYSTEM_INSTRUCTION, supportedAreas), message);
        } catch (GeminiClient.GeminiApiException e) {
            throw new BusinessException(CourseErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            boolean isTravelRelated = node.path("isTravelRelated").asBoolean(true);
            if (!isTravelRelated) {
                return new ParsedMessage(false, CourseIntent.empty());
            }
            return new ParsedMessage(true, toIntent(node));
        } catch (Exception e) {
            log.error("Intent 파싱 실패 | Gemini 응답: {}", json, e);
            throw new BusinessException(CourseErrorCode.AI_SERVICE_UNAVAILABLE);
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

        JsonNode nameHints = node.path("spotNameHints");
        String anchorName = textOrNull(node, "anchor");

        return new CourseIntent(
                startArea,
                node.path("nearbyAllowed").asBoolean(false),
                intOrNull(node, "budget"),
                textOrNull(node, "companion"),
                stringList(node, "mood"),
                slotHints,
                false,
                pins(node, "includeSpots", nameHints),
                pins(node, "excludeSpots", nameHints),
                anchorName != null ? CourseAnchor.of(anchorName) : null
        );
    }

    /**
     * 장소명 배열 → 미해결 SpotPin 목록. 원문은 그대로 name에, AI의 공식 명칭 추측은 힌트로만 붙인다.
     * 빈 문자열은 버린다 (SpotPin이 빈 이름을 거부한다).
     */
    private List<SpotPin> pins(JsonNode node, String field, JsonNode hints) {
        return stringList(node, field).stream()
                .filter(name -> !name.isBlank())
                .map(name -> SpotPin.of(name, hints.isObject() ? textOrNull(hints, name) : null))
                .toList();
    }

    /** 문자열 배열 필드 — 배열이 아니거나 없으면 빈 목록. 원소 정규화(공백·중복)는 CourseIntent가 한다 */
    private List<String> stringList(JsonNode node, String field) {
        JsonNode array = node.path(field);
        if (!array.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        array.forEach(v -> {
            if (v.isTextual()) values.add(v.asText());
        });
        return values;
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
