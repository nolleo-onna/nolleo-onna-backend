package com.nolleo.onna.domain.course.application.service;

import java.util.List;
import java.util.function.Function;

/**
 * 제목 검색 결과로 "확정 / 후보 여러 개 / 없음"을 가르는 규칙 — 기준점과 지정 장소가 같은 규칙을 쓴다.
 *
 *   - 결과가 없으면 NONE
 *   - 사용자가 말한 이름과 제목이 (공백·대소문자 무시) 정확히 같은 결과가 있으면 그것으로 확정 — 여럿이면 DB 정렬 첫 번째
 *   - 결과가 하나뿐이면 확정
 *   - 그 외(부분 일치만 여러 개)는 AMBIGUOUS — 사용자에게 골라달라고 묻는다
 */
final class TitleMatch {

    private TitleMatch() {
    }

    record Result<T>(T resolved, List<T> candidates) {
        static <T> Result<T> none() { return new Result<>(null, List.of()); }
        static <T> Result<T> resolved(T value) { return new Result<>(value, List.of()); }
        static <T> Result<T> ambiguous(List<T> candidates) { return new Result<>(null, List.copyOf(candidates)); }

        boolean isResolved() { return resolved != null; }
        boolean isAmbiguous() { return resolved == null && !candidates.isEmpty(); }
        boolean isNone() { return resolved == null && candidates.isEmpty(); }
    }

    static <T> Result<T> decide(String name, List<T> matches, Function<T, String> title) {
        if (matches.isEmpty()) return Result.none();
        String wanted = compact(name);
        for (T match : matches) {
            if (compact(title.apply(match)).equals(wanted)) return Result.resolved(match);
        }
        if (matches.size() == 1) return Result.resolved(matches.get(0));
        return Result.ambiguous(matches);
    }

    static String compact(String text) {
        return text == null ? "" : text.replace(" ", "").toLowerCase();
    }
}
