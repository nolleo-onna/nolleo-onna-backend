package com.nolleo.onna.domain.course.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 코스의 기준점 — "부산국제항만컨퍼런스 근처 갈만한 곳"처럼 사용자가 "X 근처"라고 말한 X.
 *
 *   name          사용자가 말한 이름 원문
 *   source        어디서 찾았는지 (EVENT: 행사 데이터, SPOT: 관광 스팟). 못 찾았으면 null
 *   contentId     찾은 행사·스팟 식별자
 *   matchedTitle  찾은 행사·스팟 제목 — "무엇으로 이해했는지" 보여주는 용도
 *   point         기준 좌표. 있으면 검색 중심과 첫 구간 거리의 기준이 지역 중심 대신 이 좌표가 된다
 *   period        행사 기간 표시용 ("10.14~10.16"). 스팟이면 null
 *
 * 파서는 name만 채우고, CourseAnchorResolver가 행사 → 스팟 순으로 찾아 나머지를 채운다.
 * 위치는 데이터에서만 온다 — AI가 "어디쯤일 것"이라고 추측한 좌표는 쓰지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseAnchor(
        String name,
        AnchorSource source,
        String contentId,
        String matchedTitle,
        GeoPoint point,
        String period
) {
    public enum AnchorSource { EVENT, SPOT }

    public CourseAnchor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("기준점 이름은 비어 있을 수 없습니다.");
        }
        name = name.strip();
    }

    /** 아직 찾지 않은 기준점 — 파서가 만든다 */
    public static CourseAnchor of(String name) {
        return new CourseAnchor(name, null, null, null, null, null);
    }

    public boolean isResolved() {
        return point != null;
    }

    public CourseAnchor resolvedTo(AnchorSource source, String contentId, String matchedTitle, GeoPoint point, String period) {
        return new CourseAnchor(name, source, contentId, matchedTitle, point, period);
    }

    /** 확인 문구용 — "부산국제항만컨퍼런스(부산항국제전시컨벤션센터, 10.14~10.16)" 형태. 제목이 원문과 같으면 원문만 */
    public String displayName() {
        if (!isResolved()) return name;
        StringBuilder sb = new StringBuilder(name);
        boolean sameTitle = matchedTitle == null
                || matchedTitle.replace(" ", "").equalsIgnoreCase(name.replace(" ", ""));
        if (!sameTitle || period != null) {
            sb.append("(");
            if (!sameTitle) sb.append(matchedTitle);
            if (period != null) sb.append(sameTitle ? "" : ", ").append(period);
            sb.append(")");
        }
        return sb.toString();
    }
}
