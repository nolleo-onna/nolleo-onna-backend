package com.nolleo.onna.domain.generatedcourse.domain.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * 사용자 입력 지역명(signgu) → 지도 중심 좌표 + mp_map_places.district 필터값 매핑.
 * PostGIS 미사용 환경에서 지역 시작점을 하드코딩으로 제공한다.
 */
public enum DistrictCenter {

    HAEUNDAE ("해운대", 35.1631, 129.1639, "해운대구"),
    GWANGAN  ("광안리", 35.1531, 129.1187, "수영구"),
    SEOMYEON ("서면",   35.1577, 129.0597, "부산진구"),
    JEONPO   ("전포",   35.1536, 129.0587, "부산진구"),
    YEONGDO  ("영도",   35.0888, 129.0712, "영도구"),
    NAMPO    ("남포동", 35.0978, 129.0300, "중구"),
    CENTUM   ("센텀",   35.1690, 129.1302, "해운대구"),
    SONGDO   ("송도",   35.0741, 129.0154, "서구"),
    GIJANG   ("기장",   35.2444, 129.2144, "기장군"),
    SASANG   ("사상",   35.1497, 128.9931, "사상구");

    private final String signgu;
    private final double latitude;
    private final double longitude;
    private final String district;

    DistrictCenter(String signgu, double latitude, double longitude, String district) {
        this.signgu    = signgu;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.district  = district;
    }

    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDistrict()  { return district; }
    public String getSigngu()    { return signgu; }

    public static Optional<DistrictCenter> of(String signgu) {
        if (signgu == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(d -> d.signgu.equals(signgu.trim()))
                .findFirst();
    }
}
