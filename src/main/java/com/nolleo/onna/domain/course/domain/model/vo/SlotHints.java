package com.nolleo.onna.domain.course.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 사용자가 자연어로 표현한 슬롯 구성 힌트.
 * 예: "점심 저녁 카페 + 관광지 한 군데" → foodCount=2, cafeCount=1, attractionCount=1
 * 언급하지 않은 항목은 null (0이 아님 — 0은 "빼달라"는 명시적 의도).
 *
 * @JsonIgnoreProperties: isEmpty() 파생 getter가 직렬화 시 "empty" 필드로 함께 저장되는데,
 * 생성자 파라미터가 아니므로 역직렬화 시 무시해야 한다 (Redis 대화 상태, JSONB intent 스냅샷 양쪽에 영향).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlotHints(
        Integer foodCount,
        Integer cafeCount,
        Integer attractionCount,
        Integer activityCount
) {
    /** 아무 힌트도 없는 상태 — SlotPlanner가 기본 패턴을 적용한다 */
    public boolean isEmpty() {
        return foodCount == null && cafeCount == null
                && attractionCount == null && activityCount == null;
    }
}
