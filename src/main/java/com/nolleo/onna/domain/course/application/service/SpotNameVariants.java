package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 사용자가 말한 장소명으로 DB 제목 검색을 시도할 키워드 목록 — 시도 순서대로.
 *
 *   1. 원문 그대로                     "광안리 바다"
 *   2. 동의어 치환                     "광안리 해수욕장"   (바다·바닷가·해변·비치 → 해수욕장, 마을 → 문화마을)
 *   3. 흔한 접미어 제거               "태종대 공원" → "태종대"  (제목 포함 검색이라 짧아져도 안전하다)
 *   4. AI가 추측한 공식 명칭 힌트      "광안리해수욕장"
 *
 * 판단은 언제나 DB가 한다 — 여기서는 검색어만 늘린다. 첫 단계에서 찾으면 뒤 단계는 시도하지 않으므로
 * 정상적인 이름은 조회 1회로 끝난다. 동의어·접미어 표는 우리 데이터 기준으로 늘려간다.
 */
final class SpotNameVariants {

    /** 왼쪽 표현이 이름 안에 있으면 오른쪽으로 바꾼 변형을 추가한다 — 시도 순서가 의미 있으므로 List로 둔다 (긴 표현 먼저) */
    private static final List<Map.Entry<String, String>> SYNONYMS = List.of(
            Map.entry("바닷가", "해수욕장"),
            Map.entry("바다", "해수욕장"),
            Map.entry("해변", "해수욕장"),
            Map.entry("비치", "해수욕장"),
            Map.entry("마을", "문화마을")
    );

    /** 이름 끝에 붙는 흔한 일반어 — 떼어내도 제목 포함 검색으로 찾을 수 있다 */
    private static final List<String> GENERIC_SUFFIXES = List.of("공원", "유원지", "관광지", "입구", "앞");

    private SpotNameVariants() {
    }

    static List<String> of(SpotPin pin) {
        Set<String> variants = new LinkedHashSet<>();
        String name = pin.name();
        variants.add(name);

        for (Map.Entry<String, String> synonym : SYNONYMS) {
            String from = synonym.getKey();
            String to = synonym.getValue();
            if (name.contains(from) && !name.contains(to)) variants.add(name.replace(from, to));
        }

        for (String suffix : GENERIC_SUFFIXES) {
            String stripped = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()).strip() : null;
            if (stripped != null && !stripped.isEmpty()) variants.add(stripped);
        }

        if (pin.officialNameHint() != null) variants.add(pin.officialNameHint());
        return List.copyOf(variants);
    }
}
