package com.nolleo.onna.domain.map.domain.model.vo;

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