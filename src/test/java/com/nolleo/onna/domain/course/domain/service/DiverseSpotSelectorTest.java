package com.nolleo.onna.domain.course.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class DiverseSpotSelectorTest {

    /** 후보 — id와 세부 분류만 있으면 된다 */
    private record Spot(String id, String sub) {
    }

    private static Spot spot(String id) {
        return new Spot(id, null);
    }

    private static Spot spot(String id, String sub) {
        return new Spot(id, sub);
    }

    /** 난수가 항상 0 → 가중치와 무관하게 남은 후보 중 첫 번째(최상위)를 뽑는다 */
    private static final RandomGenerator ALWAYS_FIRST = () -> 0L;

    private static List<Spot> select(DiverseSpotSelector selector, List<Spot> ranked, int count, Set<String> penalized) {
        return selector.select(ranked, count, Spot::id, Spot::sub, penalized);
    }

    private static List<Spot> tenSpots() {
        return List.of(spot("s0"), spot("s1"), spot("s2"), spot("s3"), spot("s4"),
                spot("s5"), spot("s6"), spot("s7"), spot("s8"), spot("s9"));
    }

    /** 같은 조건으로 trials번 1개씩 뽑아 id별 빈도를 센다 */
    private static Map<String, Integer> frequencies(DiverseSpotSelector selector, List<Spot> ranked,
                                                    Set<String> penalized, int trials) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < trials; i++) {
            String id = select(selector, ranked, 1, penalized).get(0).id();
            counts.merge(id, 1, Integer::sum);
        }
        return counts;
    }

    @Test
    @DisplayName("시드가 같으면 같은 결과, 시드가 다르면 조합이 달라질 수 있다 — 같은 입력에 항상 같은 코스가 나오지 않는다")
    void select_isReproducibleBySeed_andVariesAcrossSeeds() {
        List<Spot> first = select(new DiverseSpotSelector(new Random(7)), tenSpots(), 3, Set.of());
        List<Spot> again = select(new DiverseSpotSelector(new Random(7)), tenSpots(), 3, Set.of());
        assertThat(again).isEqualTo(first);

        Set<List<Spot>> combos = new java.util.HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            combos.add(select(new DiverseSpotSelector(new Random(seed)), tenSpots(), 3, Set.of()));
        }
        assertThat(combos.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("상위 순위가 더 자주 뽑히지만 하위 순위도 가끔 뽑힌다 (순위 가중 샘플링)")
    void select_prefersHigherRank_butNotExclusively() {
        Map<String, Integer> counts = frequencies(new DiverseSpotSelector(new Random(42)), tenSpots(), Set.of(), 3000);

        int top = counts.getOrDefault("s0", 0);
        int bottom = counts.getOrDefault("s9", 0);
        // 가중치 비율 exp(9/8) ≈ 3.1배 — 여유를 두고 2배 이상, 그리고 꼴찌도 0이 아니어야 한다
        assertThat(top).isGreaterThan(bottom * 2);
        assertThat(bottom).isPositive();
    }

    @Test
    @DisplayName("같은 세부 분류는 1곳까지만 먼저 뽑고, 슬롯이 남으면 그때 중복을 허용한다")
    void select_limitsSubCategory_thenAllowsDuplicates() {
        DiverseSpotSelector selector = new DiverseSpotSelector(ALWAYS_FIRST);
        List<Spot> ranked = List.of(spot("beach1", "해변"), spot("beach2", "해변"), spot("park", "공원"));

        // 2곳: 해변 1 + 공원 1 (2위 해변은 건너뜀)
        assertThat(select(selector, ranked, 2, Set.of())).extracting(Spot::id).containsExactly("beach1", "park");
    }

    @Test
    @DisplayName("세부 분류가 전부 겹쳐 슬롯을 못 채우면 중복을 허용해 채운다 — 세부 분류 null은 제한 대상이 아니다")
    void select_fillsWithDuplicates_whenAllSubCategoriesUsed() {
        DiverseSpotSelector selector = new DiverseSpotSelector(ALWAYS_FIRST);
        List<Spot> ranked = List.of(spot("beach1", "해변"), spot("beach2", "해변"), spot("beach3", "해변"),
                spot("unknown", null), spot("beach4", "해변"));

        // 3곳: 해변 1 → null은 언제나 가능 → 남은 건 전부 해변이라 2단계에서 beach2
        assertThat(select(selector, ranked, 3, Set.of())).extracting(Spot::id)
                .containsExactly("beach1", "unknown", "beach2");
    }

    @Test
    @DisplayName("최근 이력 스팟은 덜 뽑히지만, 후보가 그것뿐이면 그대로 뽑힌다 (감점이지 제외가 아니다)")
    void select_penalizesRecent_butStillPicksWhenOnlyCandidate() {
        List<Spot> ranked = List.of(spot("recent"), spot("fresh"));

        // 1위 recent(감점 0.2)와 2위 fresh(exp(-1/8) ≈ 0.88) — 감점이 없다면 1위가 더 자주 나와야 하지만 감점 후엔 2위가 더 자주 나온다
        Map<String, Integer> counts = frequencies(new DiverseSpotSelector(new Random(1)), ranked, Set.of("recent"), 2000);
        assertThat(counts.getOrDefault("fresh", 0)).isGreaterThan(counts.getOrDefault("recent", 0));

        assertThat(select(new DiverseSpotSelector(new Random(1)), List.of(spot("recent")), 1, Set.of("recent")))
                .extracting(Spot::id).containsExactly("recent");
    }

    @Test
    @DisplayName("count가 0 이하이거나 후보가 없으면 빈 목록, count가 후보 수 이상이면 전부 돌려준다")
    void select_edges() {
        DiverseSpotSelector selector = new DiverseSpotSelector(new Random(3));
        List<Spot> ranked = List.of(spot("a", "x"), spot("b", "x"));

        assertThat(select(selector, ranked, 0, Set.of())).isEmpty();
        assertThat(select(selector, List.of(), 2, Set.of())).isEmpty();
        assertThat(select(selector, ranked, 2, Set.of())).containsExactlyElementsOf(ranked);
        assertThat(select(selector, ranked, 5, Set.of())).containsExactlyElementsOf(ranked);
    }

    @Test
    @DisplayName("뽑힌 후보는 서로 다르다 (비복원 추출)")
    void select_neverRepeatsACandidate() {
        for (long seed = 0; seed < 30; seed++) {
            List<Spot> picked = select(new DiverseSpotSelector(new Random(seed)), tenSpots(), 5, Set.of());
            assertThat(picked).hasSize(5).doesNotHaveDuplicates();
        }
    }
}
