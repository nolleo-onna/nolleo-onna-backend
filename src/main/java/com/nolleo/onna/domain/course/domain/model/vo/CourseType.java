package com.nolleo.onna.domain.course.domain.model.vo;

/**
 * 코스 유형 — ALGORITHM 모드 전용. AI 모드는 사용하지 않는다(null).
 * 유형별 제목·설명 생성 규칙을 도메인 지식으로 보유한다.
 */
public enum CourseType {

    ACTIVE("액티브") {
        @Override
        public String buildDescription(String companion) {
            return withCompanion(companion, "체험·레저를 즐기는 액티브 코스입니다.");
        }
    },
    CULTURE("문화 탐방") {
        @Override
        public String buildDescription(String companion) {
            return withCompanion(companion, "자연, 전시, 역사를 둘러보는 문화 탐방 코스입니다.");
        }
    },
    FOOD_TOUR("맛집 투어") {
        @Override
        public String buildDescription(String companion) {
            return withCompanion(companion, "맛집과 카페를 중심으로 즐기는 식도락 코스입니다.");
        }
    };

    private final String label;

    CourseType(String label) {
        this.label = label;
    }

    /** 코스 제목 생성 — "{startArea} {유형라벨} 코스" */
    public String buildTitle(String startArea) {
        return startArea + " " + label + " 코스";
    }

    /** 코스 설명 생성 — 동행자가 있으면 문구에 포함 */
    public abstract String buildDescription(String companion);

    /** companion null 허용 — 없으면 동행 문구 생략 */
    static String withCompanion(String companion, String body) {
        if (companion == null || companion.isBlank()) return body;
        return companion + "과(와) 함께 " + body;
    }
}
