package com.nolleo.onna.domain.external;

/**
 * 부산 주요 지역 정보.
 *   districtName - 행정구 이름 (날씨/혼잡도 캐시 key에 사용)
 *   nx, ny       - 기상청 초단기실황 격자 좌표
 *   areaCd       - TourAPI 관광지 지역코드 (부산=6)
 *   signguCd     - TourAPI 시군구코드 (참고문서 '관광지시군구코드정보' 기준)
 */
public enum BusanDistrict {

    HAEUNDAE ("해운대구",  98, 76, "6", "23"),
    GWANGAN  ("수영구",    98, 75, "6", "20"),
    SEOMYEON ("부산진구",  97, 74, "6", "14"),
    YEONGDO  ("영도구",    97, 73, "6", "12"),
    NAMPO    ("중구",      97, 74, "6", "11"),
    GIJANG   ("기장군",   100, 78, "6", "31"),
    DONGNAE  ("동래구",    98, 76, "6", "15"),
    BUJIN    ("북구",      96, 76, "6", "17"),
    SAHA     ("사하구",    95, 73, "6", "19"),
    GANGSEO  ("강서구",    93, 74, "6", "21"),
    YEONJE   ("연제구",    98, 75, "6", "16"),
    SASANG   ("사상구",    95, 75, "6", "22");

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
