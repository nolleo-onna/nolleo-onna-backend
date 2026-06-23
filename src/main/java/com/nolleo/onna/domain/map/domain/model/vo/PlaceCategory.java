package com.nolleo.onna.domain.map.domain.model.vo;

/**
 * 지도 장소 카테고리 코드
 *
 * <ul>
 *   <li>FD — 음식점/카페</li>
 *   <li>VE — 전시/공연</li>
 *   <li>NA — 자연/공원</li>
 *   <li>HS — 역사/문화유산</li>
 *   <li>EX — 체험/액티비티</li>
 *   <li>LS — 레저스포츠</li>
 * </ul>
 *
 * FOOD 타입은 항상 FD로 고정되며,
 * SPOT 타입은 sp_spots.lcls_systm1 값에서 매핑됩니다.
 * 매핑 불가 코드(예: AC=숙박)는 null 처리되어 mp_map_places에서 제외됩니다.
 */
public enum PlaceCategory {
    FD,   // 음식점/카페
    VE,   // 전시/공연
    NA,   // 자연/공원
    HS,   // 역사/문화유산
    EX,   // 체험/액티비티
    LS;   // 레저스포츠

    public static PlaceCategory fromSpotLclsSystm1(String lclsSystm1) {
        if (lclsSystm1 == null) return null;
        try {
            return PlaceCategory.valueOf(lclsSystm1.trim());
        } catch (IllegalArgumentException e) {
            return null;  // AC(숙박) 등 지원하지 않는 코드는 null
        }
    }
}
