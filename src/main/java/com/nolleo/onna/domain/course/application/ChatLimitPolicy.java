package com.nolleo.onna.domain.course.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI 챗봇 대화 비용 상한 정책 (app.chat.*).
 *
 * 사용자에게 보이는 한도는 일일 코스 생성 횟수(CourseGenerationLimiter.DAILY_LIMIT)뿐이고,
 * 아래 값들은 평소엔 체감하지 않는 비용 안전장치다 — 매 턴 Gemini 파싱이 나가므로
 * "생성 없이 메시지만 계속 보내는" 경우의 상한을 잡는다.
 *
 * @param maxTurnsPerConversation 한 대화에서 받아주는 최대 메시지 수. 초과하면 대화를 종료한다.
 *                                마지막 턴에서는 선호 되묻기를 건너뛰고 바로 생성 확인을 묻는다.
 * @param maxOffTopicStreak       같은 대화에서 여행 무관 메시지가 이 횟수 연속되면 대화를 종료한다.
 * @param dailyMessageLimit       회원별 하루 최대 메시지 수(KST). 초과 메시지는 파싱 전에 거절해 비용을 쓰지 않는다.
 */
@ConfigurationProperties(prefix = "app.chat")
public record ChatLimitPolicy(
        @DefaultValue("10") int maxTurnsPerConversation,
        @DefaultValue("3") int maxOffTopicStreak,
        @DefaultValue("40") int dailyMessageLimit
) {
}
