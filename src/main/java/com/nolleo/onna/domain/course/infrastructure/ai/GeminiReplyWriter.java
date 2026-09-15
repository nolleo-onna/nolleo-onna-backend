package com.nolleo.onna.domain.course.infrastructure.ai;

import com.nolleo.onna.domain.course.application.port.ChatReplyWriter;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatReplyWriter의 Gemini 구현.
 * 되묻기 질문과 완료 안내를 자연스러운 문장으로 만든다.
 * Gemini 호출 실패 시 고정 문구로 폴백 — 흐름이 끊기지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiReplyWriter implements ChatReplyWriter {

    private final GeminiClient geminiClient;

    private static final String SYSTEM_INSTRUCTION = """
            너는 부산 여행 코스 추천 챗봇이다. 친근하고 간결한 한국어 존댓말로 답한다.
            이모지는 최대 1개. 두 문장 이내. 질문 외 다른 설명 금지.
            """;

    /** 여행 코스 요청과 무관한 메시지 — 고정 문구 (AI 호출 없이 즉시 응답) */
    @Override
    public String offTopic() {
        return "저는 부산 여행 코스를 추천해드리는 챗봇이에요! 어디로 떠나고 싶으신가요? 😊";
    }

    /** 대화당 턴 상한 — 고정 문구. 종료 안내는 AI에 맡기지 않는다(비용·문구 일관성) */
    @Override
    public String turnLimitReached() {
        return "대화가 길어져서 여기서 마칠게요. 새 대화에서 지역·동행·분위기를 한 번에 말씀해주시면 바로 만들어드릴게요! 🙏";
    }

    /** 여행 무관 메시지 연속 상한 — 고정 문구 */
    @Override
    public String offTopicLimitReached() {
        return "여행 코스 이야기가 이어지지 않아 대화를 마칠게요. 새 대화에서 부산 어디로 떠날지 알려주세요! 😊";
    }

    /** 일일 메시지 상한 — 고정 문구 */
    @Override
    public String messageLimitReached(int dailyLimit) {
        return "오늘 챗봇에 보낼 수 있는 메시지 수(하루 " + dailyLimit + "개)를 모두 사용하셨어요. 내일 다시 이용해주세요!";
    }

    /**
     * startArea가 없을 때 — 지역 되묻기.
     * 기준점을 말했는데 데이터에서 못 찾았으면 그 사실을 고정 문구로 알린다 — 못 찾은 이름을 AI가 다르게 옮기면 안 되기 때문이다.
     */
    @Override
    public String askStartArea(CourseIntent intent) {
        if (intent.hasUnresolvedAnchor()) {
            return String.format("'%s'을(를) 행사·장소 데이터에서 찾지 못했어요. 부산 어느 지역 근처인지 알려주시면 " +
                    "그 지역 기준으로 만들어드릴게요! (예: 해운대, 광안리, 서면)", intent.anchor().name());
        }
        try {
            return geminiClient.generateText(SYSTEM_INSTRUCTION,
                    "사용자가 시작 지역을 말하지 않았다. 부산 어느 지역에서 시작할지 물어봐라. " +
                    "예시 지역(해운대, 광안리, 서면)을 자연스럽게 포함해라.");
        } catch (Exception e) {
            log.warn("되묻기 문구 생성 실패 — 기본 문구 사용: {}", e.getMessage());
            return "부산 어느 지역에서 시작하실 건가요? (예: 해운대, 광안리, 서면)";
        }
    }

    /**
     * 선택 필드(budget/companion/mood)가 전부 없을 때 — 묶어서 1회 되묻기.
     * 이 시점에 이미 맞춘 기준점·지정 장소가 있으면 요약(🎯/📍/⚠️)을 함께 보여준다 —
     * 확인 단계까지 기다리지 않고 한 턴 먼저 오매칭을 잡을 수 있게.
     */
    @Override
    public String askPreferences(CourseIntent intent) {
        String question;
        try {
            question = geminiClient.generateText(SYSTEM_INSTRUCTION, String.format(
                    "사용자가 %s에서 여행 코스를 원한다. 더 잘 맞춰주기 위해 " +
                    "동행(누구와 가는지), 예산, 원하는 분위기를 한 번에 가볍게 물어봐라. " +
                    "그냥 추천해달라고 해도 된다는 안내도 포함해라. " +
                    "장소 지정(꼭 넣을 곳·뺄 곳)이나 기준점은 언급하지 마라 — 별도로 안내가 붙는다.", intent.startArea()));
        } catch (Exception e) {
            log.warn("되묻기 문구 생성 실패 — 기본 문구 사용: {}", e.getMessage());
            question = String.format("%s 코스를 준비할게요! 누구와 가시나요? 예산이나 원하는 분위기가 있다면 알려주세요. " +
                    "(그냥 '추천해줘'라고 하시면 바로 만들어드려요)", intent.startArea());
        }
        return question + anchorSummary(intent) + pinSummary(intent);
    }

    /** 생성 확인 대기 중, 실제로 생성을 시작시키는 정확한 트리거 문구 — CourseChatService와 반드시 일치해야 한다. */
    private static final String GENERATE_TRIGGER_GUIDE = "\n\"코스 생성 시작\"이라고 정확히 말씀해주시면 바로 만들어드릴게요!";

    /**
     * 필수·선택 정보가 모두 모였을 때 — 생성 확인.
     * 지정 장소 안내(무엇으로 이해했는지 / 못 찾은 이름)와 트리거 문구 안내는 AI 호출 성공 여부와 무관하게
     * 항상 고정 형식으로 덧붙인다 — 매칭 결과를 AI가 바꿔 말하거나 지어내면 안 되기 때문이다.
     */
    @Override
    public String confirmGenerate(CourseIntent intent) {
        String question;
        try {
            question = geminiClient.generateText(SYSTEM_INSTRUCTION, String.format(
                    "사용자가 %s에서 여행 코스를 원한다. 동행: %s, 예산: %s, 분위기: %s. " +
                    "지금까지 파악한 조건을 한 문장으로 요약해서 생성해도 될지 물어봐라. " +
                    "장소 지정(꼭 넣을 곳·뺄 곳)과 \"코스 생성 시작\"이라는 문구는 언급하지 마라 — 별도로 안내가 붙는다.",
                    intent.startArea(),
                    intent.companion() != null ? intent.companion() : "미정",
                    intent.budget() != null ? intent.budget() + "원" : "자유",
                    intent.mood().isEmpty() ? "미정" : String.join(", ", intent.mood())));
        } catch (Exception e) {
            log.warn("생성 확인 문구 생성 실패 — 기본 문구 사용: {}", e.getMessage());
            question = String.format("%s 코스, 지금까지 말씀해주신 조건으로 생성할까요?", intent.startArea());
        }
        return question + anchorSummary(intent) + pinSummary(intent) + GENERATE_TRIGGER_GUIDE;
    }

    /**
     * 기준점 안내 — 고정 형식.
     *   🎯 부산불꽃축제(제20회 부산불꽃축제, 11.1~11.1) 행사가 있어요 — 이 근처, 광안리 기준으로 만들게요   (행사)
     *   🎯 기준점: 광안리 바다(광안리해수욕장) 근처 · 광안리 기준                                          (스팟)
     *   ⚠️ '부산국제항만컨퍼런스'은(는) 찾지 못해 광안리 중심으로 잡았어요   (지역은 직접 말한 경우)
     * 행사는 "있어?"라고 물어본 경우에도 답이 되도록 존재를 먼저 말해준다.
     */
    static String anchorSummary(CourseIntent intent) {
        if (intent.anchor() == null) return "";
        if (!intent.anchor().isResolved()) {
            return String.format("\n⚠️ '%s'은(는) 찾지 못해 %s 중심으로 잡았어요", intent.anchor().name(), intent.startArea());
        }
        if (intent.anchor().source() == CourseAnchor.AnchorSource.EVENT) {
            return String.format("\n🎯 %s 행사가 있어요 — 이 근처, %s 기준으로 만들게요", intent.anchor().displayName(), intent.startArea());
        }
        return String.format("\n🎯 기준점: %s 근처 · %s 기준", intent.anchor().displayName(), intent.startArea());
    }

    /**
     * 지정 장소 안내 — 고정 형식.
     *   📍 꼭 넣을 곳: 광안리해수욕장(광안리 바다) · 뺄 곳: 해운대해수욕장
     *   ⚠️ 찾지 못한 곳: 동백섬 바다 — 정확한 이름을 알려주시면 반영할게요
     * 매칭된 제목이 사용자가 말한 이름과 다르면 괄호로 원문을 함께 보여줘 오해를 잡을 수 있게 한다.
     */
    static String pinSummary(CourseIntent intent) {
        if (!intent.hasSpotPins()) return "";

        List<String> includes = intent.includeSpots().stream().filter(SpotPin::isResolved).map(SpotPin::displayName).toList();
        List<String> excludes = intent.excludeSpots().stream().filter(SpotPin::isResolved).map(SpotPin::displayName).toList();
        List<String> unresolved = new ArrayList<>();
        intent.includeSpots().stream().filter(pin -> !pin.isResolved()).map(SpotPin::name).forEach(unresolved::add);
        intent.excludeSpots().stream().filter(pin -> !pin.isResolved()).map(SpotPin::name).forEach(unresolved::add);

        StringBuilder sb = new StringBuilder();
        if (!includes.isEmpty() || !excludes.isEmpty()) {
            List<String> parts = new ArrayList<>();
            if (!includes.isEmpty()) parts.add("꼭 넣을 곳: " + String.join(", ", includes));
            if (!excludes.isEmpty()) parts.add("뺄 곳: " + String.join(", ", excludes));
            sb.append("\n📍 ").append(String.join(" · ", parts));
        }
        if (!unresolved.isEmpty()) {
            sb.append("\n⚠️ 찾지 못한 곳: ").append(String.join(", ", unresolved))
              .append(" — 정확한 장소 이름을 알려주시면 반영할게요 (예: 광안리해수욕장)");
        }
        return sb.toString();
    }

    /** 생성 완료 안내 (임시 — 실제 코스 요약 반영은 추후 개선) */
    @Override
    public String ready(CourseIntent intent) {
        return String.format("%s 코스를 만들었어요!", intent.startArea());
    }
}
