package com.nolleo.onna.domain.course.infrastructure.ai;

import com.nolleo.onna.domain.course.application.port.ChatReplyWriter;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    /** startArea가 없을 때 — 지역 되묻기 */
    @Override
    public String askStartArea() {
        try {
            return geminiClient.generateText(SYSTEM_INSTRUCTION,
                    "사용자가 시작 지역을 말하지 않았다. 부산 어느 지역에서 시작할지 물어봐라. " +
                    "예시 지역(해운대, 광안리, 서면)을 자연스럽게 포함해라.");
        } catch (Exception e) {
            log.warn("되묻기 문구 생성 실패 — 기본 문구 사용: {}", e.getMessage());
            return "부산 어느 지역에서 시작하실 건가요? (예: 해운대, 광안리, 서면)";
        }
    }

    /** 선택 필드(budget/companion/mood)가 전부 없을 때 — 묶어서 1회 되묻기 */
    @Override
    public String askPreferences(CourseIntent intent) {
        try {
            return geminiClient.generateText(SYSTEM_INSTRUCTION, String.format(
                    "사용자가 %s에서 여행 코스를 원한다. 더 잘 맞춰주기 위해 " +
                    "동행(누구와 가는지), 예산, 원하는 분위기를 한 번에 가볍게 물어봐라. " +
                    "그냥 추천해달라고 해도 된다는 안내도 포함해라.", intent.startArea()));
        } catch (Exception e) {
            log.warn("되묻기 문구 생성 실패 — 기본 문구 사용: {}", e.getMessage());
            return String.format("%s 코스를 준비할게요! 누구와 가시나요? 예산이나 원하는 분위기가 있다면 알려주세요. " +
                    "(그냥 '추천해줘'라고 하시면 바로 만들어드려요)", intent.startArea());
        }
    }

    /** 생성 확인 대기 중, 실제로 생성을 시작시키는 정확한 트리거 문구 — CourseChatService와 반드시 일치해야 한다. */
    private static final String GENERATE_TRIGGER_GUIDE = " \"코스 생성 시작\"이라고 정확히 말씀해주시면 바로 만들어드릴게요!";

    /** 필수·선택 정보가 모두 모였을 때 — 생성 확인. 트리거 문구 안내는 AI 호출 성공 여부와 무관하게 항상 고정으로 덧붙인다. */
    @Override
    public String confirmGenerate(CourseIntent intent) {
        String question;
        try {
            question = geminiClient.generateText(SYSTEM_INSTRUCTION, String.format(
                    "사용자가 %s에서 여행 코스를 원한다. 동행: %s, 예산: %s, 분위기: %s. " +
                    "지금까지 파악한 조건을 한 문장으로 요약해서 생성해도 될지 물어봐라. " +
                    "\"코스 생성 시작\"이라는 문구는 언급하지 마라 — 별도로 안내가 붙는다.",
                    intent.startArea(),
                    intent.companion() != null ? intent.companion() : "미정",
                    intent.budget() != null ? intent.budget() + "원" : "자유",
                    intent.mood().isEmpty() ? "미정" : String.join(", ", intent.mood())));
        } catch (Exception e) {
            log.warn("생성 확인 문구 생성 실패 — 기본 문구 사용: {}", e.getMessage());
            question = String.format("%s 코스, 지금까지 말씀해주신 조건으로 생성할까요?", intent.startArea());
        }
        return question + GENERATE_TRIGGER_GUIDE;
    }

    /** 생성 완료 안내 (임시 — 실제 코스 요약 반영은 추후 개선) */
    @Override
    public String ready(CourseIntent intent) {
        return String.format("%s 코스를 만들었어요!", intent.startArea());
    }
}
