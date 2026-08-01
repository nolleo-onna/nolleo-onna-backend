package com.nolleo.onna.domain.course.domain.model.vo;

import java.util.List;

/**
 * 코스 생성 의도 — 자연어/폼 등 모든 입력이 수렴하는 정규화된 VO.
 *
 * 필드 정책:
 *   startArea      필수. 없으면 코스 생성 불가 → 되묻기
 *   nearbyAllowed  자동 추출 ("근처도 괜찮아")
 *   budget         선택. null = 무제한
 *   companion      선택. null = 생략
 *   mood           선택. empty = 벡터 리랭킹 스킵
 *   slotHints      자동 추출. isEmpty = 기본 패턴
 *   clarifiedOnce  선택 필드 되묻기를 이미 했는지 — 어떤 응답이 와도 되묻기는 1회만
 */
public record CourseIntent(
        String startArea,
        boolean nearbyAllowed,
        Integer budget,
        String companion,
        List<String> mood,
        SlotHints slotHints,
        boolean clarifiedOnce
) {
    public CourseIntent {
        if (mood == null) mood = List.of();
        if (slotHints == null) slotHints = new SlotHints(null, null, null, null);
    }

    /** 여행 주제와 무관한 메시지 파싱 시 사용하는 빈 intent */
    public static CourseIntent empty() {
        return new CourseIntent(null, false, null, null, List.of(), null, false);
    }

    /** 코스 생성 가능 여부 — startArea만 있으면 생성한다 */
    public boolean canGenerate() {
        return startArea != null && !startArea.isBlank();
    }

    /** 선택 필드(budget/companion/mood)가 전부 비어 있고, 아직 되묻지 않았는지 */
    public boolean needsClarification() {
        boolean allEmpty = budget == null && companion == null && mood.isEmpty();
        return allEmpty && !clarifiedOnce;
    }

    /** 되묻기를 완료한 상태로 표시 — 이후에는 무조건 생성으로 진행 */
    public CourseIntent markClarified() {
        return new CourseIntent(startArea, nearbyAllowed, budget, companion, mood, slotHints, true);
    }

    /**
     * 후속 대화에서 파싱된 intent를 병합한다.
     * 새 값이 있으면 덮어쓰고, 없으면(null/empty) 기존 값을 유지한다.
     */
    public CourseIntent merge(CourseIntent newer) {
        return new CourseIntent(
                newer.startArea != null && !newer.startArea.isBlank() ? newer.startArea : this.startArea,
                this.nearbyAllowed || newer.nearbyAllowed,
                newer.budget != null ? newer.budget : this.budget,
                newer.companion != null ? newer.companion : this.companion,
                !newer.mood.isEmpty() ? newer.mood : this.mood,
                !newer.slotHints.isEmpty() ? newer.slotHints : this.slotHints,
                this.clarifiedOnce || newer.clarifiedOnce
        );
    }
}
