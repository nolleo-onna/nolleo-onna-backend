package com.nolleo.onna.domain.course.domain.model.vo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 코스 생성 의도 — 자연어/폼 등 모든 입력이 수렴하는 정규화된 VO.
 *
 * 필드 정책:
 *   startArea      필수. 없으면 코스 생성 불가 → 되묻기. 기준점(anchor)이 찾아지면 그 좌표에 가장 가까운 지역으로 자동 채워진다
 *   nearbyAllowed  자동 추출 ("근처도 괜찮아") — 후보 스팟 검색 반경을 넓힌다 (CourseGenerationService)
 *   budget         선택. null = 무제한. 스냅샷으로 저장만 하고 생성 로직(후보 선택·가격 필터)에는 아직 반영하지 않는다
 *   companion      선택. null = 생략
 *   mood           선택. empty = 벡터 리랭킹 스킵
 *   slotHints      자동 추출. isEmpty = 기본 패턴
 *   clarifiedOnce  선택 필드 되묻기를 이미 했는지 — 어떤 응답이 와도 되묻기는 1회만
 *   includeSpots   선택. 코스에 꼭 넣어달라고 한 장소("광안리 해수욕장은 꼭 넣어줘"). 사용자가 말한 이름을 키로 보관하고,
 *                  실제 스팟(content_id) 매칭 결과는 생성 확인 시점에 SpotPin에 채워진다 (SpotPinResolver)
 *   excludeSpots   선택. 코스에서 빼달라고 한 장소("해운대 해수욕장은 빼줘"). 정책은 includeSpots와 같다
 *   anchor         선택. "X 근처"의 X — 행사·스팟 데이터에서 찾은 좌표가 있으면 검색 중심이 지역 중심 대신 그 좌표가 된다
 *
 * includeSpots·excludeSpots는 이름 기준으로 서로 배타적이다 — 나중에 말한 쪽이 이긴다 (merge).
 */
public record CourseIntent(
        String startArea,
        boolean nearbyAllowed,
        Integer budget,
        String companion,
        List<String> mood,
        SlotHints slotHints,
        boolean clarifiedOnce,
        List<SpotPin> includeSpots,
        List<SpotPin> excludeSpots,
        CourseAnchor anchor
) {
    public CourseIntent {
        if (mood == null) mood = List.of();
        if (slotHints == null) slotHints = new SlotHints(null, null, null, null);
        includeSpots = dedupeByName(includeSpots);
        excludeSpots = dedupeByName(excludeSpots);
    }

    /** 장소 지정·기준점이 없는 intent — 폼 입력 등 장소명을 다루지 않는 호출부용 편의 생성자 */
    public CourseIntent(String startArea, boolean nearbyAllowed, Integer budget, String companion,
                        List<String> mood, SlotHints slotHints, boolean clarifiedOnce) {
        this(startArea, nearbyAllowed, budget, companion, mood, slotHints, clarifiedOnce, List.of(), List.of(), null);
    }

    /** 기준점 없이 장소 지정만 있는 intent 편의 생성자 */
    public CourseIntent(String startArea, boolean nearbyAllowed, Integer budget, String companion,
                        List<String> mood, SlotHints slotHints, boolean clarifiedOnce,
                        List<SpotPin> includeSpots, List<SpotPin> excludeSpots) {
        this(startArea, nearbyAllowed, budget, companion, mood, slotHints, clarifiedOnce, includeSpots, excludeSpots, null);
    }

    /** 여행 주제와 무관한 메시지 파싱 시 사용하는 빈 intent */
    public static CourseIntent empty() {
        return new CourseIntent(null, false, null, null, List.of(), null, false, List.of(), List.of(), null);
    }

    /** 코스 생성 가능 여부 — startArea만 있으면 생성한다 */
    public boolean canGenerate() {
        return startArea != null && !startArea.isBlank();
    }

    /** 선택 필드(budget/companion/mood)가 전부 비어 있고, 아직 되묻지 않았는지 */
    public boolean needsClarification() {
        boolean allEmpty = budget == null && companion == null && mood.isEmpty();
        return allEmpty && !clarifiedOnce;
    }

    /** 사용자가 이름으로 지정한 장소(포함 또는 제외)가 하나라도 있는지 */
    public boolean hasSpotPins() {
        return !includeSpots.isEmpty() || !excludeSpots.isEmpty();
    }

    /** 아직 스팟과 맞추지 않은 지정이 하나라도 있는지 — 확인 시점 매칭이 필요한지 판단 */
    public boolean hasUnresolvedPins() {
        return includeSpots.stream().anyMatch(pin -> !pin.isResolved())
                || excludeSpots.stream().anyMatch(pin -> !pin.isResolved());
    }

    /** 기준점을 말했지만 아직(또는 끝내) 데이터에서 찾지 못한 상태인지 */
    public boolean hasUnresolvedAnchor() {
        return anchor != null && !anchor.isResolved();
    }

    /**
     * 후보 검색·거리 계산의 중심 좌표 — 기준점이 찾아졌으면 그 좌표, 아니면 시작 지역 중심.
     * 둘 다 없으면 empty (코스를 만들 수 없는 상태).
     */
    public Optional<GeoPoint> center() {
        if (anchor != null && anchor.isResolved()) return Optional.of(anchor.point());
        return DistrictCenter.of(startArea).map(DistrictCenter::toGeoPoint);
    }

    /** 장소 지정만 바꾼 사본 — 매칭 결과를 채울 때 쓴다 */
    public CourseIntent withPins(List<SpotPin> include, List<SpotPin> exclude) {
        return new CourseIntent(startArea, nearbyAllowed, budget, companion, mood, slotHints, clarifiedOnce,
                include, exclude, anchor);
    }

    /** 기준점만 바꾼 사본 */
    public CourseIntent withAnchor(CourseAnchor anchor) {
        return new CourseIntent(startArea, nearbyAllowed, budget, companion, mood, slotHints, clarifiedOnce,
                includeSpots, excludeSpots, anchor);
    }

    /** 시작 지역만 바꾼 사본 — 기준점 좌표에서 지역을 계산해 채울 때 쓴다 */
    public CourseIntent withStartArea(String startArea) {
        return new CourseIntent(startArea, nearbyAllowed, budget, companion, mood, slotHints, clarifiedOnce,
                includeSpots, excludeSpots, anchor);
    }

    /** 되묻기를 완료한 상태로 표시 — 이후에는 무조건 생성으로 진행 */
    public CourseIntent markClarified() {
        return new CourseIntent(startArea, nearbyAllowed, budget, companion, mood, slotHints, true,
                includeSpots, excludeSpots, anchor);
    }

    /**
     * 후속 대화에서 파싱된 intent를 병합한다.
     * 새 값이 있으면 덮어쓰고, 없으면(null/empty) 기존 값을 유지한다.
     *
     * 장소 지정은 덮어쓰지 않고 누적한다 — "A 넣어줘" 다음 턴에 "B도 넣어줘"면 둘 다 남는다.
     * 같은 이름을 다시 말하면 이미 매칭해 둔 기존 pin을 유지한다(재조회 불필요).
     * 같은 이름이 반대편에 새로 오면 나중에 말한 쪽으로 옮긴다 — "A 넣어줘" 뒤에 "A는 빼줘"면 제외로 간다.
     * 기준점은 하나뿐이라 새로 말한 것이 이전 것을 대체한다. 같은 이름이면 이미 찾아둔 기존 기준점을 유지한다.
     */
    public CourseIntent merge(CourseIntent newer) {
        return new CourseIntent(
                newer.startArea != null && !newer.startArea.isBlank() ? newer.startArea : this.startArea,
                this.nearbyAllowed || newer.nearbyAllowed,
                newer.budget != null ? newer.budget : this.budget,
                newer.companion != null ? newer.companion : this.companion,
                !newer.mood.isEmpty() ? newer.mood : this.mood,
                !newer.slotHints.isEmpty() ? newer.slotHints : this.slotHints,
                this.clarifiedOnce || newer.clarifiedOnce,
                unionMinus(this.includeSpots, newer.includeSpots, newer.excludeSpots),
                unionMinus(this.excludeSpots, newer.excludeSpots, newer.includeSpots),
                mergeAnchor(this.anchor, newer.anchor)
        );
    }

    private static CourseAnchor mergeAnchor(CourseAnchor base, CourseAnchor newer) {
        if (newer == null) return base;
        if (base != null && base.name().equals(newer.name())) return base;
        return newer;
    }

    /** base ∪ added 에서 removed(이름 기준)를 뺀 목록 — 순서는 처음 등장한 순서, 같은 이름은 base 쪽(매칭 결과 보존)을 남긴다 */
    private static List<SpotPin> unionMinus(List<SpotPin> base, List<SpotPin> added, List<SpotPin> removed) {
        Map<String, SpotPin> byName = new LinkedHashMap<>();
        base.forEach(pin -> byName.put(pin.name(), pin));
        added.forEach(pin -> byName.putIfAbsent(pin.name(), pin));
        removed.forEach(pin -> byName.remove(pin.name()));
        return List.copyOf(byName.values());
    }

    /** null 원소 제거, 같은 이름은 처음 것만 남긴다 (등장 순서 유지). null 목록은 빈 목록 */
    private static List<SpotPin> dedupeByName(List<SpotPin> pins) {
        if (pins == null || pins.isEmpty()) return List.of();
        Map<String, SpotPin> byName = new LinkedHashMap<>();
        for (SpotPin pin : pins) {
            if (pin != null) byName.putIfAbsent(pin.name(), pin);
        }
        return List.copyOf(byName.values());
    }
}
