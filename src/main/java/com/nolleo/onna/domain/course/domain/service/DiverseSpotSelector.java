package com.nolleo.onna.domain.course.domain.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * 순위가 매겨진 후보 풀에서 count개를 "다양하게" 고른다 — 같은 입력이라도 매번 다른 조합이 나오게 하기 위한 선택기.
 *
 * 앞에서부터 count개를 자르면(기존 방식) 지역·예산이 같을 때 항상 같은 스팟만 나온다. 대신:
 *   1. 순위 가중 랜덤 샘플링 — 순위 i(0부터)의 가중치는 exp(-i / RANK_DECAY_TAU). 1위가 뽑힐 확률이 가장 높지만 확정은 아니다.
 *      순위는 호출자가 정한다 (폼: 거리순, 챗봇: 리랭킹순).
 *   2. 세부 분류 중복 제한 — 같은 세부 분류(subCategory)는 1곳까지만 먼저 뽑고, 그래도 슬롯이 남을 때만 중복을 허용한다.
 *      관광지 3곳이 해수욕장 3개로 채워지는 것을 막는다. 세부 분류가 null인 후보는 제한 대상이 아니다.
 *   3. 최근 이력 감점 — penalizedIds(최근 코스에 담겼던 스팟)는 가중치를 RECENT_PENALTY배로 낮춘다.
 *      하드 제외가 아니라 감점이므로 후보가 그것뿐이면 그대로 뽑힌다.
 *
 * 난수원은 주입받는다 — 테스트는 시드를 고정하거나 nextLong()이 0을 돌려주는 생성기(항상 1순위 선택)를 넣어 결과를 고정한다.
 * 도메인 타입에 묶이지 않도록 제네릭이며, 후보의 id·세부 분류는 함수로 받는다.
 */
public class DiverseSpotSelector {

    /** 순위 감쇠 상수 — 8이면 8위의 가중치가 1위의 약 37%, 20위가 약 8%, 40위가 약 0.7% */
    static final double RANK_DECAY_TAU = 8.0;

    /** 최근 코스에 담겼던 스팟의 가중치 배율 */
    static final double RECENT_PENALTY = 0.2;

    private final RandomGenerator random;

    public DiverseSpotSelector(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * @param ranked        순위순 후보 (앞이 상위). 비어 있으면 빈 목록
     * @param count         뽑을 개수. count가 후보 수 이상이면 전부 돌려준다
     * @param idOf          후보 식별자 — penalizedIds와 대조
     * @param subCategoryOf 후보의 세부 분류 — null이면 중복 제한 대상이 아님
     * @param penalizedIds  감점할 후보 식별자 집합 (최근 이력)
     * @return 뽑힌 순서대로의 후보. 방문 순서는 호출자(CourseAssembler)가 다시 정한다
     */
    public <T> List<T> select(List<T> ranked, int count,
                              Function<T, String> idOf, Function<T, String> subCategoryOf,
                              Set<String> penalizedIds) {
        if (count <= 0 || ranked.isEmpty()) return List.of();
        if (count >= ranked.size()) return List.copyOf(ranked);

        List<Weighted<T>> remaining = new ArrayList<>(ranked.size());
        for (int rank = 0; rank < ranked.size(); rank++) {
            T item = ranked.get(rank);
            double weight = Math.exp(-rank / RANK_DECAY_TAU);
            if (penalizedIds.contains(idOf.apply(item))) weight *= RECENT_PENALTY;
            remaining.add(new Weighted<>(item, subCategoryOf.apply(item), weight));
        }

        List<T> picked = new ArrayList<>(count);
        Set<String> usedSubCategories = new HashSet<>();
        while (picked.size() < count && !remaining.isEmpty()) {
            // 1단계: 아직 안 뽑은 세부 분류만. 전부 겹치면 2단계: 중복 허용
            List<Weighted<T>> eligible = remaining.stream()
                    .filter(w -> w.subCategory == null || !usedSubCategories.contains(w.subCategory))
                    .toList();
            if (eligible.isEmpty()) eligible = remaining;

            Weighted<T> chosen = draw(eligible);
            remaining.remove(chosen);
            picked.add(chosen.item);
            if (chosen.subCategory != null) usedSubCategories.add(chosen.subCategory);
        }
        return picked;
    }

    /** 가중치에 비례해 하나를 뽑는다 (룰렛 휠). 난수가 0이면 첫 후보, 1에 가까우면 마지막 후보 */
    private <T> Weighted<T> draw(List<Weighted<T>> eligible) {
        double total = 0;
        for (Weighted<T> w : eligible) total += w.weight;

        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (Weighted<T> w : eligible) {
            cumulative += w.weight;
            if (r < cumulative) return w;
        }
        return eligible.get(eligible.size() - 1);
    }

    private record Weighted<T>(T item, String subCategory, double weight) {
    }
}
