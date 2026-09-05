package com.nolleo.onna.domain.post.domain.model.vo;

public enum PostCategoryTag {
    CAFE("카페"),
    RESTAURANT("맛집"),
    EXHIBITION("전시/공연"),
    NATURE("자연/공원"),
    HISTORY("역사/문화"),
    ACTIVITY("체험/액티비티"),
    LEISURE("레저/스포츠"),
    FESTIVAL("축제"),
    MARKET("시장"),
    TOURIST_SPOT("관광지"),
    NIGHT_VIEW("야경"),
    DRIVE("드라이브"),
    DATE("데이트"),
    FAMILY("가족여행"),
    SOLO("혼행"),
    PET("반려동물");

    private final String label;

    PostCategoryTag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
