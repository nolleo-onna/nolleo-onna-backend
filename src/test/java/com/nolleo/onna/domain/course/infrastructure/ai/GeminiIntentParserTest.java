package com.nolleo.onna.domain.course.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.ParsedMessage;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Gemini 응답 JSON → CourseIntent 변환 규칙을 검증한다. Gemini 호출 자체는 GeminiClient 목으로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class GeminiIntentParserTest {

    @Mock GeminiClient geminiClient;

    private GeminiIntentParser parser;

    @BeforeEach
    void setUp() {
        parser = new GeminiIntentParser(geminiClient, new ObjectMapper());
    }

    private void stubGemini(String json) {
        given(geminiClient.generateJson(anyString(), anyString())).willReturn(json);
    }

    @Test
    @DisplayName("꼭 넣을 장소·뺄 장소를 includeSpots / excludeSpots로 읽는다")
    void parse_readsIncludeAndExcludeSpots() {
        stubGemini("""
                {"isTravelRelated":true,"startArea":"광안리","nearbyAllowed":false,"budget":null,"companion":"연인",
                 "mood":["로맨틱한"],"slotHints":{"foodCount":null,"cafeCount":null,"attractionCount":1,"activityCount":null},
                 "includeSpots":["광안리 해수욕장"],"excludeSpots":["해운대 해수욕장"]}
                """);

        ParsedMessage parsed = parser.parse("광안리 해수욕장은 꼭 넣고 해운대 해수욕장은 빼줘, 연인이랑 관광지 한 곳");

        assertThat(parsed.isTravelRelated()).isTrue();
        assertThat(parsed.intent().startArea()).isEqualTo("광안리");
        assertThat(parsed.intent().includeSpots()).containsExactly(SpotPin.of("광안리 해수욕장"));
        assertThat(parsed.intent().excludeSpots()).containsExactly(SpotPin.of("해운대 해수욕장"));
        assertThat(parsed.intent().hasUnresolvedPins()).isTrue(); // 파서는 매칭하지 않는다 — 확인 시점에 SpotPinResolver가 한다
        assertThat(parsed.intent().mood()).containsExactly("로맨틱한");
        assertThat(parsed.intent().slotHints().attractionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("장소 지정 배열이 없거나 배열이 아니면 빈 목록으로 읽고, 문자열이 아닌 원소는 무시한다")
    void parse_toleratesMissingOrMalformedSpotArrays() {
        stubGemini("""
                {"isTravelRelated":true,"startArea":"서면","nearbyAllowed":false,"budget":null,"companion":null,
                 "mood":[],"slotHints":{},"includeSpots":"광안리 해수욕장","excludeSpots":[1, null, "  ", "동백섬"]}
                """);

        ParsedMessage parsed = parser.parse("서면");

        assertThat(parsed.intent().includeSpots()).isEmpty();
        assertThat(parsed.intent().excludeSpots()).extracting(SpotPin::name).containsExactly("동백섬");
    }

    @Test
    @DisplayName("공식 명칭 추측은 힌트로만 붙이고 원문은 그대로 두며, 기준점(anchor)은 미해결 CourseAnchor로 읽는다")
    void parse_readsHintsAndAnchor() {
        stubGemini("""
                {"isTravelRelated":true,"startArea":null,"nearbyAllowed":false,"budget":null,"companion":null,
                 "mood":[],"slotHints":{},"includeSpots":["광안리 바다"],"excludeSpots":[],
                 "spotNameHints":{"광안리 바다":"광안리해수욕장"},"anchor":"부산국제항만컨퍼런스"}
                """);

        ParsedMessage parsed = parser.parse("부산국제항만컨퍼런스 근처, 광안리 바다는 꼭");

        assertThat(parsed.intent().includeSpots()).containsExactly(SpotPin.of("광안리 바다", "광안리해수욕장"));
        assertThat(parsed.intent().includeSpots().get(0).name()).isEqualTo("광안리 바다");
        assertThat(parsed.intent().anchor()).isEqualTo(CourseAnchor.of("부산국제항만컨퍼런스"));
        assertThat(parsed.intent().hasUnresolvedAnchor()).isTrue();
        assertThat(parsed.intent().startArea()).isNull(); // 기준점 위치는 추측하지 않는다 — 데이터에서 찾는다
    }

    @Test
    @DisplayName("지원하지 않는 지역명은 startArea를 null로 정규화한다")
    void parse_nullifiesUnsupportedArea() {
        stubGemini("""
                {"isTravelRelated":true,"startArea":"강릉","nearbyAllowed":false,"budget":null,"companion":null,
                 "mood":[],"slotHints":{},"includeSpots":[],"excludeSpots":[]}
                """);

        ParsedMessage parsed = parser.parse("강릉 가고 싶어");

        assertThat(parsed.intent().startArea()).isNull();
        assertThat(parsed.intent().canGenerate()).isFalse();
    }

    @Test
    @DisplayName("여행 무관 메시지는 isTravelRelated=false와 빈 intent로 돌려준다")
    void parse_offTopic() {
        stubGemini("""
                {"isTravelRelated":false,"startArea":null,"nearbyAllowed":false,"budget":null,"companion":null,
                 "mood":[],"slotHints":{},"includeSpots":[],"excludeSpots":[]}
                """);

        ParsedMessage parsed = parser.parse("오늘 주식 어때?");

        assertThat(parsed.isTravelRelated()).isFalse();
        assertThat(parsed.intent().hasSpotPins()).isFalse();
    }

    @Test
    @DisplayName("Gemini 호출 실패나 JSON 아닌 응답은 AI_SERVICE_UNAVAILABLE(503)로 변환한다")
    void parse_translatesFailuresTo503() {
        given(geminiClient.generateJson(anyString(), anyString()))
                .willThrow(new GeminiClient.GeminiApiException("timeout"))
                .willReturn("이건 JSON이 아니에요");

        assertThatThrownBy(() -> parser.parse("광안리"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.AI_SERVICE_UNAVAILABLE);
        assertThatThrownBy(() -> parser.parse("광안리"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.AI_SERVICE_UNAVAILABLE);
    }
}
