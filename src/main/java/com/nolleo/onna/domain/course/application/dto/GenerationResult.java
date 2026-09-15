package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.Course;

/**
 * 코스 생성 결과 + 생성 중 내린 판단.
 *   budgetFilterRelaxed — 예산 가격 필터를 걸었더니 식사·카페 후보가 슬롯보다 적어 필터를 풀고 채웠는지
 */
public record GenerationResult(Course course, boolean budgetFilterRelaxed) {
}
