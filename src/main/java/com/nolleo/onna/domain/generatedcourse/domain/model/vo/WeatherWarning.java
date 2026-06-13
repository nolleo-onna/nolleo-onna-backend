package com.nolleo.onna.domain.generatedcourse.domain.model.vo;

/** 코스 아이템별 날씨 경고 정보 묶음 */
public record WeatherWarning(
        /** 날씨 위험 경고 여부 */
        boolean hasWarning,
        /** 사용자에게 노출할 날씨 경고 메시지 — hasWarning=true 일 때만 유효한 값을 가짐 */
        String message
) {
    /** compact constructor — 자가 유효성 검사 */
    public WeatherWarning {
        if (hasWarning && (message == null || message.isBlank())) {
            throw new IllegalArgumentException("날씨 경고 메시지는 비어 있을 수 없습니다.");
        }
    }

    public static WeatherWarning none() {
        return new WeatherWarning(false, null);
    }

    public static WeatherWarning of(String message) {
        return new WeatherWarning(true, message);
    }
}
