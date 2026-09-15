package com.nolleo.onna.domain.course.application.dto;

import java.util.List;
import java.util.Optional;

/**
 * 이름 매칭 결과가 여러 개라 사용자에게 골라달라고 물어둔 항목 — Redis 대화 상태에 저장된다.
 *
 *   kind        무엇을 고르는지 (ANCHOR: "X 근처"의 기준점, INCLUDE: 꼭 넣을 곳). 뺄 곳은 묻지 않고 전부 뺀다
 *   name        사용자가 말한 원문 ("부산국제", "해수욕장")
 *   candidates  추천 순(정확도 → 거리/시작일)으로 정렬된 후보. 1번이 추천 1순위 — 사용자가 고르지 않으면 이걸 쓴다
 *
 * 다음 턴에 ChoiceSelector가 번호·이름으로 선택을 읽어 확정한다.
 */
public record PendingChoice(Kind kind, String name, List<Candidate> candidates) {

    public enum Kind {
        ANCHOR("기준점"), INCLUDE("꼭 넣을 곳");

        private final String label;

        Kind(String label) { this.label = label; }

        public String label() { return label; }
    }

    /**
     * 후보 하나. latitude/longitude는 기준점 후보가 확정될 때 좌표로 쓰인다.
     * detail은 사용자가 고르기 쉽게 붙이는 짧은 설명 — 행사는 기간("10.14~10.16"), 스팟은 거리("0.3km")
     * source는 기준점 후보의 출처(EVENT/SPOT), 스팟 지정 후보는 null
     */
    public record Candidate(String contentId, String title, Double latitude, Double longitude, String detail, String source) {
    }

    public PendingChoice {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("후보가 없는 선택지는 만들 수 없습니다.");
        }
        candidates = List.copyOf(candidates);
    }

    /** 추천 1순위 — 사용자가 고르지 않았을 때의 기본값 */
    public Candidate first() {
        return candidates.get(0);
    }

    /** 1부터 시작하는 번호로 선택. 범위 밖이면 empty */
    public Optional<Candidate> pick(int number) {
        if (number < 1 || number > candidates.size()) return Optional.empty();
        return Optional.of(candidates.get(number - 1));
    }
}
