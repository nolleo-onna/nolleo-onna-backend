package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpotNameVariantsTest {

    @Test
    @DisplayName("원문 → 동의어 치환 → 접미어 제거 → AI 힌트 순서로 검색어를 만든다 (중복 제거)")
    void of_buildsVariantsInOrder() {
        assertThat(SpotNameVariants.of(SpotPin.of("광안리 바다", "광안리해수욕장")))
                .containsExactly("광안리 바다", "광안리 해수욕장", "광안리해수욕장");
        assertThat(SpotNameVariants.of(SpotPin.of("태종대 공원")))
                .containsExactly("태종대 공원", "태종대");
        assertThat(SpotNameVariants.of(SpotPin.of("감천마을")))
                .containsExactly("감천마을", "감천문화마을");
    }

    @Test
    @DisplayName("이미 공식 표현이거나 바꿀 것이 없으면 원문 하나만 시도한다 — 정상적인 이름은 조회 1회로 끝난다")
    void of_singleVariant_whenNothingToExpand() {
        assertThat(SpotNameVariants.of(SpotPin.of("광안리 해수욕장"))).containsExactly("광안리 해수욕장");
        assertThat(SpotNameVariants.of(SpotPin.of("감천문화마을"))).containsExactly("감천문화마을");
    }

    @Test
    @DisplayName("AI 힌트가 원문이나 다른 변형과 같으면 중복으로 추가하지 않는다")
    void of_dedupesHint() {
        assertThat(SpotNameVariants.of(SpotPin.of("자갈치시장", "자갈치시장"))).containsExactly("자갈치시장");
    }
}
