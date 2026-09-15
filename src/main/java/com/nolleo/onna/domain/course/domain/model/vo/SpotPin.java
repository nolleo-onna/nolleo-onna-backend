package com.nolleo.onna.domain.course.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 사용자가 이름으로 지정한 장소 (코스에 꼭 넣을 곳 / 뺄 곳).
 *
 *   name              사용자가 말한 이름 원문 ("광안리 바다") — 대화 중 병합·충돌 판단의 키. 절대 바꾸지 않는다
 *   officialNameHint  AI가 추측한 공식 명칭 ("광안리해수욕장"). 검색 키워드 후보일 뿐 판단 근거가 아니다 — 실제 매칭은 DB가 한다
 *   contentId         매칭된 스팟(sp_spots.content_id). 아직 매칭하지 않았거나 못 찾았으면 null
 *   matchedTitle      매칭된 스팟의 제목 — 확인 문구에서 "무엇으로 이해했는지" 보여주는 용도
 *
 * 파서는 name(+hint)만 채운 미해결 pin을 만들고, 생성 확인 시점에 SpotPinResolver가 DB에서 찾아 contentId·matchedTitle을 채운다.
 *
 * @JsonIgnoreProperties: isResolved() 파생 getter가 "resolved" 필드로 직렬화되는데 생성자 파라미터가 아니라
 * 역직렬화 때 무시해야 한다 (Redis 대화 상태, JSONB intent 스냅샷 양쪽).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotPin(String name, String officialNameHint, String contentId, String matchedTitle) {

    public SpotPin {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("장소명은 비어 있을 수 없습니다.");
        }
        name = name.strip();
        if (officialNameHint != null && officialNameHint.isBlank()) officialNameHint = null;
    }

    /** 이미 매칭된 pin을 힌트 없이 만드는 편의 생성자 (테스트·복원용) */
    public SpotPin(String name, String contentId, String matchedTitle) {
        this(name, null, contentId, matchedTitle);
    }

    /** 아직 스팟과 맞추지 않은 지정 */
    public static SpotPin of(String name) {
        return new SpotPin(name, null, null, null);
    }

    /** 아직 스팟과 맞추지 않은 지정 + AI가 추측한 공식 명칭 힌트 */
    public static SpotPin of(String name, String officialNameHint) {
        return new SpotPin(name, officialNameHint, null, null);
    }

    public boolean isResolved() {
        return contentId != null;
    }

    public SpotPin resolvedTo(String contentId, String matchedTitle) {
        return new SpotPin(name, officialNameHint, contentId, matchedTitle);
    }

    /**
     * 확인 문구용 표시 이름 — 매칭된 제목이 사용자가 말한 이름과 사실상 같으면(공백·대소문자 차이) 제목만,
     * 다르면 "광안리해수욕장(광안리 바다)"처럼 무엇을 무엇으로 이해했는지 함께 보여준다.
     */
    public String displayName() {
        if (matchedTitle == null) return name;
        return compact(matchedTitle).equals(compact(name)) ? matchedTitle : matchedTitle + "(" + name + ")";
    }

    private static String compact(String text) {
        return text.replace(" ", "").toLowerCase();
    }
}
