package com.nolleo.onna.domain.generatedcourse.domain.service;

import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseType;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 코스 타입과 duration에 따라 슬롯 패턴을 정의하고,
 * 장소 풀에서 슬롯에 맞는 장소를 선택하여 코스 아이템 목록을 반환한다.
 */
public class CourseComposer {

    // 카테고리 그룹 정의
    private static final Set<PlaceCategory> FOOD_CATS   = EnumSet.of(PlaceCategory.FD);
    private static final Set<PlaceCategory> CULTURE_CATS = EnumSet.of(PlaceCategory.NA, PlaceCategory.VE, PlaceCategory.HS);
    private static final Set<PlaceCategory> ACTIVE_CATS  = EnumSet.of(PlaceCategory.EX, PlaceCategory.LS);

    /** 카테고리별 예상 체류 시간 (분) */
    public static short durationOf(PlaceCategory category) {
        if (category == null) return 60;
        return switch (category) {
            case FD -> 60;
            case NA -> 45;
            case HS -> 60;
            case VE -> 90;
            case EX -> 150;
            case LS -> 210;
        };
    }

    private CourseComposer() {}

    /**
     * 코스 타입 + duration 조합에 맞는 장소 목록을 반환한다.
     * usedIds: 이미 다른 코스에서 사용된 mapPlace.id — 해당 장소는 제외한다.
     */
    public static List<MapPlace> compose(CourseType type, String duration,
                                          List<MapPlace> pool, Set<Long> usedIds) {
        List<SlotType> pattern = resolvePattern(type, duration);
        List<MapPlace> food    = available(pool, usedIds, FOOD_CATS);
        List<MapPlace> culture = available(pool, usedIds, CULTURE_CATS);
        List<MapPlace> active  = available(pool, usedIds, ACTIVE_CATS);

        List<MapPlace> result = new ArrayList<>();
        for (SlotType slot : pattern) {
            MapPlace picked = switch (slot) {
                case FOOD    -> pickFirst(food,    usedIds);
                case CULTURE -> pickFirst(culture, usedIds);
                case ACTIVE  -> pickFirst(active,  usedIds);
            };
            if (picked != null) {
                result.add(picked);
                usedIds.add(picked.getId());
            }
        }
        return result;
    }

    // ── private ──────────────────────────────────────────────────────────────

    private enum SlotType { FOOD, CULTURE, ACTIVE }

    /**
     * 코스 타입 + 여행 시간 조합으로 슬롯 패턴을 결정한다.
     *
     * ACTIVE:
     *   반나절 → [CULTURE, ACTIVE, FOOD]
     *   하루   → [CULTURE, FOOD, ACTIVE, FOOD]
     *
     * CULTURE:
     *   반나절 → [CULTURE, CULTURE, FOOD]
     *   하루   → [CULTURE, CULTURE, FOOD, CULTURE, FOOD]
     *
     * FOOD_TOUR:
     *   반나절 → [FOOD, CULTURE, FOOD]
     *   하루   → [FOOD, CULTURE, FOOD, CULTURE, FOOD]
     */
    private static List<SlotType> resolvePattern(CourseType type, String duration) {
        boolean fullDay = "FULL_DAY".equalsIgnoreCase(duration);
        return switch (type) {
            case ACTIVE    -> fullDay
                    ? List.of(SlotType.CULTURE, SlotType.FOOD, SlotType.ACTIVE, SlotType.FOOD)
                    : List.of(SlotType.CULTURE, SlotType.ACTIVE, SlotType.FOOD);
            case CULTURE   -> fullDay
                    ? List.of(SlotType.CULTURE, SlotType.CULTURE, SlotType.FOOD, SlotType.CULTURE, SlotType.FOOD)
                    : List.of(SlotType.CULTURE, SlotType.CULTURE, SlotType.FOOD);
            case FOOD_TOUR -> fullDay
                    ? List.of(SlotType.FOOD, SlotType.CULTURE, SlotType.FOOD, SlotType.CULTURE, SlotType.FOOD)
                    : List.of(SlotType.FOOD, SlotType.CULTURE, SlotType.FOOD);
        };
    }

    private static List<MapPlace> available(List<MapPlace> pool, Set<Long> usedIds,
                                             Set<PlaceCategory> categories) {
        return pool.stream()
                .filter(p -> p.getId() != null && !usedIds.contains(p.getId()))
                .filter(p -> categories.contains(p.getCategory()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** 리스트 맨 앞 장소를 선택하고 usedIds에서 이미 소비된 것은 건너뜀 */
    private static MapPlace pickFirst(List<MapPlace> candidates, Set<Long> usedIds) {
        for (MapPlace p : candidates) {
            if (!usedIds.contains(p.getId())) return p;
        }
        return null;
    }
}
