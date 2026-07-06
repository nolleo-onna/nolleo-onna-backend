package com.nolleo.onna.domain.external;

/**
 * 부산 주요 지역 정보.
 *   districtName - 행정구 이름 (날씨/혼잡도 캐시 key에 사용)
 *   nx, ny       - 기상청 초단기실황 격자 좌표
 *   areaCd       - TourAPI 관광지 지역코드 (부산=6)
 *   signguCd     - TourAPI 시군구코드 (참고문서 '관광지시군구코드정보' 기준)
 */
public enum BusanDistrict {

    HAEUNDAE ("해운대구",  98, 76, "26", "26350"),
    GWANGAN  ("수영구",    98, 75, "26", "26500"),
    SEOMYEON ("부산진구",  97, 74, "26", "26230"),
    YEONGDO  ("영도구",    97, 73, "26", "26200"),
    NAMPO    ("중구",      97, 74, "26", "26110"),
    GIJANG   ("기장군",   100, 78, "26", "26710"),
    DONGNAE  ("동래구",    98, 76, "26", "26260"),
    BUJIN    ("북구",      96, 76, "26", "26320"),
    SAHA     ("사하구",    95, 73, "26", "26380"),
    GANGSEO  ("강서구",    93, 74, "26", "26440"),
    YEONJE   ("연제구",    98, 75, "26", "26470"),
    SASANG   ("사상구",    95, 75, "26", "26530");

    public final String districtName;
    public final int nx;
    public final int ny;
    public final String areaCd;
    public final String signguCd;

    BusanDistrict(String districtName, int nx, int ny, String areaCd, String signguCd) {
        this.districtName = districtName;
        this.nx = nx;
        this.ny = ny;
        this.areaCd = areaCd;
        this.signguCd = signguCd;
    }
}
