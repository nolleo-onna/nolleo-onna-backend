package com.nolleo.onna.domain.spot.domain.model.vo;

/**
 * 스팟 카테고리 코드 (sp_spots.lcls_systm_1 매핑).
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
 * 매핑 불가 코드(예: AC=숙박)는 null로 처리한다.
 */
public enum SpotCategory {
    FD("음식점/카페"),
    VE("전시/공연"),
    NA("자연/공원"),
    HS("역사/문화유산"),
    EX("체험/액티비티"),
    LS("레저스포츠");

    private final String label;

    SpotCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SpotCategory fromLclsSystm1(String lclsSystm1) {
        if (lclsSystm1 == null) return null;
        try {
            return SpotCategory.valueOf(lclsSystm1.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
