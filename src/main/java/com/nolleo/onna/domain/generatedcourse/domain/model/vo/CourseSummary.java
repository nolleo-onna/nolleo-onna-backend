package com.nolleo.onna.domain.generatedcourse.domain.model.vo;

/** AI가 계산한 코스 요약 수치 묶음 */
public record CourseSummary(
        /** 예상 총 비용 (원) */
        Integer totalCost,
        /** 예상 총 소요시간 (분) */
        Integer totalMinutes,
        /** 예상 절약 금액 (원) — compared_with_travel_course_id 기준 공식 여행코스 대비 절약액 */
        Integer totalSavings
) {
    public static CourseSummary of(Integer totalCost, Integer totalMinutes, Integer totalSavings) {
        return new CourseSummary(totalCost, totalMinutes, totalSavings);
    }
}
