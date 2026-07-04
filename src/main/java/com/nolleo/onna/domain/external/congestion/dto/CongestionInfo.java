package com.nolleo.onna.domain.external.congestion.dto;

import java.util.List;

/**
 * Redis에 캐싱되는 혼잡도 정보.
 *   districtName - 구 이름
 *   rate         - 구 내 관광지 평균 집중률 (%)
 *   baseYmd      - 기준일자 (yyyyMMdd)
 *   attractions  - 구 내 관광지별 집중률 목록
 */
public record CongestionInfo(
        String districtName,
        String rate,
        String baseYmd,
        List<CongestionAttractionInfo> attractions
) {
    public static CongestionInfo empty() {
        return new CongestionInfo("정보없음", "0", "-", List.of());
    }
}
