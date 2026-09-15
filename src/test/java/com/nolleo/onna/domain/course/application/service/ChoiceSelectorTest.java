package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.PendingChoice;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChoiceSelectorTest {

    private final ChoiceSelector selector = new ChoiceSelector();

    private static final PendingChoice ANCHOR = new PendingChoice(PendingChoice.Kind.ANCHOR, "부산국제", List.of(
            new PendingChoice.Candidate("ev1", "부산국제항만컨퍼런스", 35.1587, 129.1604, "10.14~10.16", "EVENT"),
            new PendingChoice.Candidate("ev2", "부산국제영화제", 35.1690, 129.1302, "10.1~10.10", "EVENT")));

    private static final PendingChoice INCLUDE = new PendingChoice(PendingChoice.Kind.INCLUDE, "해수욕장", List.of(
            new PendingChoice.Candidate("beach", "광안리해수욕장", 35.1532, 129.1188, "0.0km", null),
            new PendingChoice.Candidate("songjeong", "송정해수욕장", 35.1785, 129.2005, "9.8km", null)));

    /** 기준점 미해결 + '해수욕장' 미해결 지정 + 지역 없음 */
    private static CourseIntent pendingIntent() {
        return new CourseIntent(null, false, null, "친구", List.of(), null, false,
                List.of(SpotPin.of("해수욕장")), List.of(), CourseAnchor.of("부산국제"));
    }

    @Test
    @DisplayName("'기준점 2, 넣을 곳 1'처럼 항목 이름 + 번호로 각각 고른다")
    void apply_labeledNumbers() {
        CourseIntent result = selector.apply(pendingIntent(), List.of(ANCHOR, INCLUDE), "기준점 2, 넣을 곳 1로 해줘");

        assertThat(result.anchor().contentId()).isEqualTo("ev2");
        assertThat(result.anchor().source()).isEqualTo(CourseAnchor.AnchorSource.EVENT);
        assertThat(result.anchor().period()).isEqualTo("10.1~10.10");
        assertThat(result.anchor().point()).isEqualTo(new GeoPoint(35.1690, 129.1302));
        assertThat(result.includeSpots().get(0).contentId()).isEqualTo("beach");
    }

    @Test
    @DisplayName("물어본 항목이 하나뿐이면 '2번' 같은 번호만으로 고른다")
    void apply_bareNumber_whenSingleChoice() {
        CourseIntent result = selector.apply(pendingIntent(), List.of(INCLUDE), "2번이요");

        assertThat(result.includeSpots().get(0).contentId()).isEqualTo("songjeong");
        assertThat(result.anchor().isResolved()).isFalse(); // 기준점은 묻지 않았으니 건드리지 않는다
    }

    @Test
    @DisplayName("후보 제목이 메시지에 들어 있으면 이름으로 고른다 (공백 무시, 가장 긴 제목 우선)")
    void apply_byTitle() {
        CourseIntent result = selector.apply(pendingIntent(), List.of(ANCHOR, INCLUDE), "영화제 말고 항만 컨퍼런스, 송정 해수욕장으로");

        assertThat(result.anchor().contentId()).isEqualTo("ev1");
        assertThat(result.includeSpots().get(0).contentId()).isEqualTo("songjeong");
    }

    @Test
    @DisplayName("선택을 읽지 못하면(추천대로, 다른 조건만 말함, 범위 밖 번호) 추천 1순위로 확정한다 — 되묻기가 생성을 막지 않는다")
    void apply_defaultsToFirst() {
        assertThat(selector.apply(pendingIntent(), List.of(ANCHOR, INCLUDE), "추천대로 해줘").anchor().contentId()).isEqualTo("ev1");
        assertThat(selector.apply(pendingIntent(), List.of(INCLUDE), "예산은 5만원").includeSpots().get(0).contentId()).isEqualTo("beach");
        assertThat(selector.apply(pendingIntent(), List.of(INCLUDE), "7번").includeSpots().get(0).contentId()).isEqualTo("beach");
    }

    @Test
    @DisplayName("기준점을 고르면 좌표를 채우고, 지역을 말하지 않았으면 좌표에 가장 가까운 지원 지역을 startArea로 넣는다")
    void apply_anchor_fillsStartArea() {
        CourseIntent result = selector.apply(pendingIntent(), List.of(ANCHOR), "1");

        assertThat(result.startArea()).isEqualTo("해운대");
        assertThat(result.center()).contains(new GeoPoint(35.1587, 129.1604));
    }

    @Test
    @DisplayName("이미 해결된 기준점·지정은 선택으로 덮어쓰지 않는다")
    void apply_doesNotOverrideResolved() {
        CourseAnchor resolved = CourseAnchor.of("부산국제").resolvedTo(CourseAnchor.AnchorSource.SPOT, "s1", "부산국제금융센터", new GeoPoint(35.1, 129.1), null);
        CourseIntent already = pendingIntent().withAnchor(resolved)
                .withPins(List.of(new SpotPin("해수욕장", "x", "이미 고른 곳")), List.of());

        CourseIntent result = selector.apply(already, List.of(ANCHOR, INCLUDE), "기준점 2, 넣을 곳 2");

        assertThat(result.anchor()).isSameAs(resolved);
        assertThat(result.includeSpots().get(0).contentId()).isEqualTo("x");
    }
}
