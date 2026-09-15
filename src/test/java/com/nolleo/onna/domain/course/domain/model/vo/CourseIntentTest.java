package com.nolleo.onna.domain.course.domain.model.vo;

import com.nolleo.onna.domain.course.infrastructure.persistence.converter.CourseIntentJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseIntentTest {

    private static List<SpotPin> pins(String... names) {
        return Arrays.stream(names).map(SpotPin::of).toList();
    }

    private static CourseIntent withPins(List<SpotPin> include, List<SpotPin> exclude) {
        return new CourseIntent("광안리", false, null, null, List.of(), null, false, include, exclude);
    }

    @Test
    @DisplayName("장소 지정 없이 만드는 편의 생성자와 empty()는 포함·제외 목록을 빈 목록으로 둔다")
    void convenienceConstructor_andEmpty_haveNoPins() {
        CourseIntent plain = new CourseIntent("광안리", false, null, null, List.of(), null, false);

        assertThat(plain.includeSpots()).isEmpty();
        assertThat(plain.excludeSpots()).isEmpty();
        assertThat(plain.hasSpotPins()).isFalse();
        assertThat(CourseIntent.empty().hasSpotPins()).isFalse();
    }

    @Test
    @DisplayName("SpotPin은 이름의 앞뒤 공백을 제거하고, 빈 이름은 거부한다")
    void spotPin_normalizesName_andRejectsBlank() {
        assertThat(SpotPin.of("  광안리 해수욕장 ").name()).isEqualTo("광안리 해수욕장");
        assertThatThrownBy(() -> SpotPin.of("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpotPin.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 이름의 지정은 처음 것만 남기고, null 원소는 버린다 (등장 순서 유지)")
    void dedupesPinsByName() {
        CourseIntent intent = withPins(
                Arrays.asList(SpotPin.of("광안리 해수욕장"), null, SpotPin.of("광안리 해수욕장"), SpotPin.of("민락수변공원")),
                null);

        assertThat(intent.includeSpots()).extracting(SpotPin::name).containsExactly("광안리 해수욕장", "민락수변공원");
        assertThat(intent.excludeSpots()).isEmpty();
        assertThat(intent.hasSpotPins()).isTrue();
        assertThat(intent.hasUnresolvedPins()).isTrue();
    }

    @Test
    @DisplayName("merge는 장소 지정을 덮어쓰지 않고 누적한다 — 다음 턴에 말한 장소가 이전 지정에 더해진다")
    void merge_accumulatesPins() {
        CourseIntent first = withPins(pins("광안리 해수욕장"), pins("해운대 해수욕장"));
        CourseIntent second = withPins(pins("민락수변공원"), pins("동백섬"));

        CourseIntent merged = first.merge(second);

        assertThat(merged.includeSpots()).extracting(SpotPin::name).containsExactly("광안리 해수욕장", "민락수변공원");
        assertThat(merged.excludeSpots()).extracting(SpotPin::name).containsExactly("해운대 해수욕장", "동백섬");
    }

    @Test
    @DisplayName("merge에서 같은 이름이 반대편으로 새로 오면 나중에 말한 쪽으로 옮긴다 — '넣어줘' 뒤 '빼줘'면 제외")
    void merge_laterSideWins_onConflict() {
        CourseIntent first = withPins(pins("광안리 해수욕장", "민락수변공원"), pins("해운대 해수욕장"));
        CourseIntent flip = withPins(pins("해운대 해수욕장"), pins("광안리 해수욕장"));

        CourseIntent merged = first.merge(flip);

        assertThat(merged.includeSpots()).extracting(SpotPin::name).containsExactly("민락수변공원", "해운대 해수욕장");
        assertThat(merged.excludeSpots()).extracting(SpotPin::name).containsExactly("광안리 해수욕장");
    }

    @Test
    @DisplayName("merge는 같은 이름을 다시 말해도 이미 매칭해 둔 기존 pin을 유지한다 (재조회 불필요)")
    void merge_keepsResolvedPin_whenSameNameRepeated() {
        SpotPin resolved = new SpotPin("광안리 바다", "c1", "광안리해수욕장");
        CourseIntent first = withPins(List.of(resolved), List.of());
        CourseIntent repeat = withPins(pins("광안리 바다"), List.of());

        CourseIntent merged = first.merge(repeat);

        assertThat(merged.includeSpots()).containsExactly(resolved);
        assertThat(merged.hasUnresolvedPins()).isFalse();
    }

    @Test
    @DisplayName("merge는 장소 지정이 없는 후속 입력에 기존 지정을 그대로 유지한다")
    void merge_keepsPins_whenNewerHasNone() {
        CourseIntent first = withPins(pins("광안리 해수욕장"), List.of());
        CourseIntent budgetOnly = new CourseIntent(null, false, 50000, null, List.of(), null, false);

        CourseIntent merged = first.merge(budgetOnly);

        assertThat(merged.includeSpots()).extracting(SpotPin::name).containsExactly("광안리 해수욕장");
        assertThat(merged.budget()).isEqualTo(50000);
    }

    @Test
    @DisplayName("markClarified와 withPins는 나머지 필드를 보존한다")
    void markClarified_andWithPins_preserveOtherFields() {
        CourseIntent intent = withPins(pins("광안리 해수욕장"), pins("해운대 해수욕장"));

        CourseIntent clarified = intent.markClarified();
        CourseIntent repinned = clarified.withPins(List.of(new SpotPin("광안리 해수욕장", "c1", "광안리해수욕장")), List.of());

        assertThat(clarified.clarifiedOnce()).isTrue();
        assertThat(clarified.includeSpots()).extracting(SpotPin::name).containsExactly("광안리 해수욕장");
        assertThat(repinned.clarifiedOnce()).isTrue();
        assertThat(repinned.startArea()).isEqualTo("광안리");
        assertThat(repinned.includeSpots().get(0).isResolved()).isTrue();
        assertThat(repinned.excludeSpots()).isEmpty();
    }

    @Test
    @DisplayName("displayName은 매칭 제목이 말한 이름과 사실상 같으면 제목만, 다르면 '제목(원문)'으로 보여준다")
    void spotPin_displayName() {
        assertThat(new SpotPin("광안리 해수욕장", "c1", "광안리해수욕장").displayName()).isEqualTo("광안리해수욕장");
        assertThat(new SpotPin("광안리 바다", "c1", "광안리해수욕장").displayName()).isEqualTo("광안리해수욕장(광안리 바다)");
        assertThat(SpotPin.of("광안리 바다").displayName()).isEqualTo("광안리 바다");
    }

    @Test
    @DisplayName("center는 기준점이 찾아졌으면 그 좌표, 아니면 시작 지역 중심, 둘 다 없으면 empty")
    void center_prefersResolvedAnchor() {
        GeoPoint venue = new GeoPoint(35.1, 129.1);
        CourseAnchor resolved = CourseAnchor.of("행사").resolvedTo(CourseAnchor.AnchorSource.EVENT, "ev1", "행사", venue, null);
        CourseIntent withAnchor = withPins(List.of(), List.of()).withAnchor(resolved);
        CourseIntent areaOnly = withPins(List.of(), List.of());
        CourseIntent nothing = new CourseIntent(null, false, null, null, List.of(), null, false).withAnchor(CourseAnchor.of("행사"));

        assertThat(withAnchor.center()).contains(venue);
        assertThat(areaOnly.center()).contains(DistrictCenter.GWANGAN.toGeoPoint());
        assertThat(nothing.center()).isEmpty();
        assertThat(nothing.hasUnresolvedAnchor()).isTrue();
    }

    @Test
    @DisplayName("merge에서 기준점은 새로 말한 것이 이전 것을 대체하되, 같은 이름이면 이미 찾아둔 기존 기준점을 유지한다")
    void merge_anchor_replacesUnlessSameName() {
        CourseAnchor resolved = CourseAnchor.of("행사").resolvedTo(CourseAnchor.AnchorSource.EVENT, "ev1", "행사", new GeoPoint(35.1, 129.1), null);
        CourseIntent base = withPins(List.of(), List.of()).withAnchor(resolved);

        assertThat(base.merge(withPins(List.of(), List.of())).anchor()).isSameAs(resolved);                       // 언급 없음 → 유지
        assertThat(base.merge(withPins(List.of(), List.of()).withAnchor(CourseAnchor.of("행사"))).anchor()).isSameAs(resolved); // 같은 이름 → 유지
        assertThat(base.merge(withPins(List.of(), List.of()).withAnchor(CourseAnchor.of("다른 행사"))).anchor())
                .isEqualTo(CourseAnchor.of("다른 행사"));                                                            // 다른 이름 → 대체
    }

    @Test
    @DisplayName("기준점을 포함한 intent는 JSON 왕복에서 그대로 보존된다")
    void json_roundTrip_keepsAnchor() {
        CourseAnchor resolved = CourseAnchor.of("행사")
                .resolvedTo(CourseAnchor.AnchorSource.EVENT, "ev1", "행사 2026", new GeoPoint(35.1, 129.1), "10.14~10.16");
        CourseIntent original = withPins(List.of(SpotPin.of("광안리 바다", "광안리해수욕장")), List.of()).withAnchor(resolved);

        CourseIntent restored = CourseIntentJson.fromJson(CourseIntentJson.toJson(original));

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("장소 지정 필드가 없던 시절의 JSONB 스냅샷도 빈 목록으로 복원된다")
    void json_legacyWithoutPins_deserializesToEmptyLists() {
        String legacy = """
                {"startArea":"광안리","nearbyAllowed":false,"budget":null,"companion":"연인",
                 "mood":["로맨틱"],"slotHints":{"foodCount":2,"cafeCount":null,"attractionCount":null,"activityCount":null},
                 "clarifiedOnce":true}
                """;

        CourseIntent restored = CourseIntentJson.fromJson(legacy);

        assertThat(restored.startArea()).isEqualTo("광안리");
        assertThat(restored.includeSpots()).isEmpty();
        assertThat(restored.excludeSpots()).isEmpty();
    }

    @Test
    @DisplayName("해결된 pin과 미해결 pin이 섞인 intent는 JSON 왕복에서 그대로 보존된다 (파생 getter 'resolved'는 무시)")
    void json_roundTrip_keepsPins() {
        CourseIntent original = withPins(
                List.of(new SpotPin("광안리 바다", "c1", "광안리해수욕장"), SpotPin.of("민락수변공원")),
                pins("해운대 해수욕장"));

        String json = CourseIntentJson.toJson(original);
        CourseIntent restored = CourseIntentJson.fromJson(json);

        assertThat(restored).isEqualTo(original);
        assertThat(restored.includeSpots().get(0).isResolved()).isTrue();
        assertThat(restored.includeSpots().get(1).isResolved()).isFalse();
    }
}
