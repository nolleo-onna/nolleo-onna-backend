package com.nolleo.onna.domain.generatedcourse.domain.model.vo;

/**
 * 코스 유형.
 * 유형별 제목·설명 생성 규칙을 도메인 지식으로 보유한다.
 */
public enum CourseType {

    ACTIVE("액티브") {
        @Override
        public String buildDescription(String companion) {
            return companion + "과(와) 함께 체험·레저를 즐기는 액티브 코스입니다.";
        }
    },
    CULTURE("문화 탐방") {
        @Override
        public String buildDescription(String companion) {
            return companion + "과(와) 함께 자연, 전시, 역사를 둘러보는 문화 탐방 코스입니다.";
        }
    },
    FOOD_TOUR("맛집 투어") {
        @Override
        public String buildDescription(String companion) {
            return companion + "과(와) 함께 맛집과 카페를 중심으로 즐기는 식도락 코스입니다.";
        }
    };

    private final String label;

    CourseType(String label) {
        this.label = label;
    }

    /** 코스 제목 생성 — "{signgu} {유형라벨} {반나절|하루} 코스" */
    public String buildTitle(String signgu, String duration) {
        String durationLabel = "FULL_DAY".equalsIgnoreCase(duration) ? "하루" : "반나절";
        return signgu + " " + label + " " + durationLabel + " 코스";
    }

    /** 코스 설명 생성 — 동행자 정보를 포함한 유형별 문구 */
    public abstract String buildDescription(String companion);
}
