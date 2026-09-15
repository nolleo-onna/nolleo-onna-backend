package com.nolleo.onna.domain.course.infrastructure.ai;

import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 생성 확인 문구의 고정 부분(지정 장소 안내 + 트리거 안내)을 검증한다. AI가 만드는 질문 문장은 GeminiClient 목으로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class GeminiReplyWriterTest {

    @Mock GeminiClient geminiClient;

    @InjectMocks GeminiReplyWriter writer;

    private static CourseIntent intent(List<SpotPin> include, List<SpotPin> exclude) {
        return new CourseIntent("광안리", false, null, "연인", List.of("로맨틱"), null, true, include, exclude);
    }

    @Test
    @DisplayName("확인 문구는 AI 질문 뒤에 매칭 결과(📍)와 못 찾은 이름(⚠️), 트리거 안내를 고정 형식으로 붙인다")
    void confirmGenerate_appendsPinSummary() {
        given(geminiClient.generateText(anyString(), anyString())).willReturn("연인과 로맨틱한 광안리 코스, 만들까요?");
        CourseIntent intent = intent(
                List.of(new SpotPin("광안리 바다", "c1", "광안리해수욕장"), SpotPin.of("동백섬 바다")),
                List.of(new SpotPin("해운대 해수욕장", "c2", "해운대해수욕장")));

        String reply = writer.confirmGenerate(intent);

        assertThat(reply)
                .startsWith("연인과 로맨틱한 광안리 코스, 만들까요?")
                .contains("📍 꼭 넣을 곳: 광안리해수욕장(광안리 바다) · 뺄 곳: 해운대해수욕장")
                .contains("⚠️ 찾지 못한 곳: 동백섬 바다 — 정확한 장소 이름을 알려주시면 반영할게요")
                .endsWith("\"코스 생성 시작\"이라고 정확히 말씀해주시면 바로 만들어드릴게요!");
    }

    @Test
    @DisplayName("AI 호출이 실패해도 폴백 질문 + 지정 장소 안내 + 트리거 안내는 그대로 붙는다")
    void confirmGenerate_fallback_keepsPinSummary() {
        given(geminiClient.generateText(anyString(), anyString())).willThrow(new GeminiClient.GeminiApiException("timeout"));
        CourseIntent intent = intent(List.of(), List.of(new SpotPin("해운대 해수욕장", "c2", "해운대해수욕장")));

        String reply = writer.confirmGenerate(intent);

        assertThat(reply)
                .startsWith("광안리 코스, 지금까지 말씀해주신 조건으로 생성할까요?")
                .contains("📍 뺄 곳: 해운대해수욕장")
                .doesNotContain("⚠️")
                .contains("\"코스 생성 시작\"");
    }

    @Test
    @DisplayName("기준점을 찾았으면 🎯 줄로 무엇으로 이해했는지와 기준 지역을, 못 찾았으면 ⚠️ 줄로 지역 중심으로 잡았음을 알린다")
    void confirmGenerate_appendsAnchorSummary() {
        given(geminiClient.generateText(anyString(), anyString())).willReturn("만들까요?");
        CourseAnchor resolved = CourseAnchor.of("부산국제항만컨퍼런스")
                .resolvedTo(CourseAnchor.AnchorSource.EVENT, "ev1", "부산국제항만컨퍼런스 2026", new GeoPoint(35.1, 129.1), "10.14~10.16");
        CourseIntent withResolved = intent(List.of(), List.of()).withAnchor(resolved);
        CourseIntent withUnresolved = intent(List.of(), List.of()).withAnchor(CourseAnchor.of("없는행사"));

        CourseAnchor spotAnchor = CourseAnchor.of("광안리 바다")
                .resolvedTo(CourseAnchor.AnchorSource.SPOT, "beach", "광안리해수욕장", new GeoPoint(35.1, 129.1), null);

        assertThat(writer.confirmGenerate(withResolved))
                .contains("🎯 부산국제항만컨퍼런스(부산국제항만컨퍼런스 2026, 10.14~10.16) 행사가 있어요 — 이 근처, 광안리 기준으로 만들게요");
        assertThat(writer.confirmGenerate(intent(List.of(), List.of()).withAnchor(spotAnchor)))
                .contains("🎯 기준점: 광안리 바다(광안리해수욕장) 근처 · 광안리 기준");
        assertThat(writer.confirmGenerate(withUnresolved))
                .contains("⚠️ '없는행사'은(는) 찾지 못해 광안리 중심으로 잡았어요");
    }

    @Test
    @DisplayName("기준점을 못 찾은 채 지역을 되물을 때는 AI를 부르지 않고 못 찾은 이름을 고정 문구로 알린다")
    void askStartArea_mentionsUnresolvedAnchor_withoutAi() {
        CourseIntent noArea = new CourseIntent(null, false, null, null, List.of(), null, false,
                List.of(), List.of(), CourseAnchor.of("없는행사"));

        String reply = writer.askStartArea(noArea);

        assertThat(reply).startsWith("'없는행사'을(를) 행사·장소 데이터에서 찾지 못했어요");
        org.mockito.Mockito.verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("선호 되묻기에도 이미 맞춘 기준점·지정 장소 요약을 붙인다 — 확인 단계보다 한 턴 먼저 오매칭을 잡는다")
    void askPreferences_appendsMatchSummary() {
        given(geminiClient.generateText(anyString(), anyString())).willReturn("누구와 가시나요?");
        CourseIntent withPins = new CourseIntent("광안리", false, null, null, List.of(), null, false,
                List.of(new SpotPin("광안리 바다", "c1", "광안리해수욕장"), SpotPin.of("동백섬 바다")), List.of());

        String reply = writer.askPreferences(withPins);

        assertThat(reply)
                .startsWith("누구와 가시나요?")
                .contains("📍 꼭 넣을 곳: 광안리해수욕장(광안리 바다)")
                .contains("⚠️ 찾지 못한 곳: 동백섬 바다")
                .doesNotContain("코스 생성 시작"); // 트리거 안내는 확인 단계에서만
        assertThat(writer.askPreferences(intent(List.of(), List.of()))).isEqualTo("누구와 가시나요?");
    }

    @Test
    @DisplayName("지정 장소가 없으면 안내 줄을 붙이지 않는다")
    void confirmGenerate_noPins_noSummaryLine() {
        given(geminiClient.generateText(anyString(), anyString())).willReturn("만들까요?");

        String reply = writer.confirmGenerate(intent(List.of(), List.of()));

        assertThat(reply).doesNotContain("📍").doesNotContain("⚠️");
        assertThat(GeminiReplyWriter.pinSummary(intent(List.of(), List.of()))).isEmpty();
    }
}
