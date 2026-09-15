package com.nolleo.onna.domain.course.application.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 제목 검색 결과로 "확정 / 못 찾음"을 가르는 규칙 — 기준점과 지정 장소가 같은 규칙을 쓴다.
 *
 *   - 사용자가 말한 이름과 제목이 (공백·대소문자 무시) 정확히 같은 결과가 있으면 그것으로 확정 — 여럿이면 DB 정렬 첫 번째
 *   - 결과가 하나뿐이면 확정
 *   - 그 외(부분 일치만 여러 개, 또는 결과 없음)는 못 찾은 것으로 본다 — "해수욕장"처럼 애매한 이름은
 *     되묻지 않고 "정확한 이름을 알려달라"고 안내한다. 정확히 말한 이름만 반영하는 단순한 규칙을 유지하기 위해서다
 */
final class TitleMatch {

    /** 정확 일치는 DB가 첫 행으로 주므로, 결과가 하나뿐인지 판단하는 데는 2행이면 충분하다 */
    static final int LOOKUP_LIMIT = 2;

    private TitleMatch() {
    }

    static <T> Optional<T> decide(String name, List<T> matches, Function<T, String> title) {
        if (matches.isEmpty()) return Optional.empty();
        String wanted = compact(name);
        for (T match : matches) {
            if (compact(title.apply(match)).equals(wanted)) return Optional.of(match);
        }
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    static String compact(String text) {
        return text == null ? "" : text.replace(" ", "").toLowerCase();
    }
}
