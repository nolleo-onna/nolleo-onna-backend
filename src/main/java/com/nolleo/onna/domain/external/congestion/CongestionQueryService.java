package com.nolleo.onna.domain.external.congestion;

import com.nolleo.onna.domain.external.BusanDistrict;
import com.nolleo.onna.domain.external.congestion.dto.CongestionAttractionInfo;
import com.nolleo.onna.domain.external.congestion.dto.CongestionInfo;
import com.nolleo.onna.domain.external.congestion.dto.CongestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CongestionQueryService {

    private final CongestionCacheService congestionCacheService;

    /** 맵 표시용: 부산 전체 지역 혼잡도 목록 */
    public List<CongestionResponse> getAllDistricts() {
        return Arrays.stream(BusanDistrict.values())
                .collect(Collectors.toMap(
                        d -> d.areaCd + ":" + d.signguCd,
                        d -> d,
                        (a, b) -> a
                ))
                .values().stream()
                .map(d -> {
                    CongestionInfo info = congestionCacheService
                            .get(d.areaCd, d.signguCd)
                            .orElse(CongestionInfo.empty());
                    return CongestionResponse.of(d.districtName, info);
                })
                .toList();
    }

    /** 맵 표시용: 특정 지역 혼잡도 */
    public CongestionResponse getByDistrict(String districtName) {
        BusanDistrict district = Arrays.stream(BusanDistrict.values())
                .filter(d -> d.districtName.equals(districtName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 지역입니다: " + districtName));
        CongestionInfo info = congestionCacheService
                .get(district.areaCd, district.signguCd)
                .orElse(CongestionInfo.empty());
        return CongestionResponse.of(district.districtName, info);
    }

    /** 코스 추천용: 스팟 이름으로 관광지별 혼잡도 조회 */
    public Optional<CongestionAttractionInfo> getByAttractionName(String lDongRegnCd,
                                                                   String lDongSignguCd,
                                                                   String spotTitle) {
        return congestionCacheService.get(lDongRegnCd, lDongSignguCd)
                .flatMap(info -> info.attractions().stream()
                        .filter(a -> a.name().contains(spotTitle) || spotTitle.contains(a.name()))
                        .findFirst());
    }

    /** 코스 추천용: 스팟의 지역코드로 구 평균 혼잡도 조회 */
    public CongestionInfo getByRegionCode(String lDongRegnCd, String lDongSignguCd) {
        return congestionCacheService.get(lDongRegnCd, lDongSignguCd)
                .orElse(CongestionInfo.empty());
    }
}
