package com.nolleo.onna.domain.post.domain.model.vo;

public enum PostDistrictTag {
    JUNG_GU("중구"),
    SEO_GU("서구"),
    DONG_GU("동구"),
    YEONGDO_GU("영도구"),
    BUSANJIN_GU("부산진구"),
    DONGNAE_GU("동래구"),
    NAM_GU("남구"),
    BUK_GU("북구"),
    HAEUNDAE_GU("해운대구"),
    SAHA_GU("사하구"),
    GEUMJEONG_GU("금정구"),
    GANGSEO_GU("강서구"),
    YEONJE_GU("연제구"),
    SUYEONG_GU("수영구"),
    SASANG_GU("사상구"),
    GIJANG_GUN("기장군");

    private final String label;

    PostDistrictTag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
