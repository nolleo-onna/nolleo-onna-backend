package com.nolleo.onna.domain.external.congestion;

import com.nolleo.onna.domain.external.BusanDistrict;
import com.nolleo.onna.domain.external.congestion.client.CongestionApiClient;
import com.nolleo.onna.domain.external.congestion.dto.CongestionAttractionInfo;
import com.nolleo.onna.domain.external.congestion.dto.CongestionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CongestionRefreshService {

    private final CongestionApiClient congestionApiClient;
    private final CongestionCacheService congestionCacheService;

    public void refresh() {
        log.info("혼잡도 캐시 갱신 시작");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        fetchAll(today);
        log.info("혼잡도 캐시 갱신 완료");
    }

    private void fetchAll(String today) {
        for (BusanDistrict district : BusanDistrict.values()) {
            try {
                List<Map<String, String>> items =
                        congestionApiClient.fetchCongestion(district.areaCd, district.signguCd);

                List<Map<String, String>> todayItems = items.stream()
                        .filter(item -> today.equals(item.get("baseYmd")))
                        .toList();

                if (todayItems.isEmpty()) {
                    log.warn("혼잡도 오늘 데이터 없음 district={}", district.districtName);
                } else {
                    CongestionInfo info = buildCongestionInfo(district, todayItems, today);
                    congestionCacheService.save(district.areaCd, district.signguCd, info);
                    log.info("혼잡도 캐시 갱신 완료 district={} 관광지={}개",
                            district.districtName, info.attractions().size());
                }
            } catch (Exception e) {
                log.error("혼잡도 캐시 갱신 실패 district={}: {}", district.districtName, e.getMessage());
            } finally {
                sleepQuietly(5000);
            }
        }
    }

    private CongestionInfo buildCongestionInfo(BusanDistrict district,
                                               List<Map<String, String>> todayItems,
                                               String today) {
        List<CongestionAttractionInfo> attractions = todayItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrDefault("tAtsNm", "")))
                .entrySet().stream()
                .filter(e -> !e.getKey().isBlank())
                .map(e -> new CongestionAttractionInfo(
                        e.getKey(),
                        avgRate(e.getValue())
                ))
                .sorted((a, b) -> Double.compare(b.rate(), a.rate()))
                .toList();

        double districtAvg = attractions.stream()
                .mapToDouble(CongestionAttractionInfo::rate)
                .average()
                .orElse(0.0);

        return new CongestionInfo(
                district.districtName,
                districtAvg,
                today,
                attractions
        );
    }

    private double avgRate(List<Map<String, String>> items) {
        return items.stream()
                .mapToDouble(item -> parseRate(item.get("cnctrRate")))
                .average()
                .orElse(0.0);
    }

    private double parseRate(String value) {
        try { return Double.parseDouble(value); }
        catch (Exception e) { return 0.0; }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
