package com.nolleo.onna.domain.generatedcourse.domain.model.vo;

import java.util.List;

/** 코스 생성 시 사용자가 입력한 조건 묶음 */
public record CourseInput(
        /** 사용자 입력 지역/시군구 */
        String signgu,
        /** 사용자 입력 예산 (원) */
        Integer budget,
        /** 사용자 입력 여행 시간 (예: "반나절", "하루") */
        String duration,
        /** 사용자 입력 동행 유형 (예: "커플", "가족") */
        String companion,
        /** 사용자 입력 분위기/태그 목록 */
        List<String> mood
) {
    public static CourseInput of(String signgu, Integer budget, String duration,
                                  String companion, List<String> mood) {
        return new CourseInput(signgu, budget, duration, companion, mood);
    }
}
