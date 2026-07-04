package com.nolleo.onna.domain.external.congestion.scheduler;

import com.nolleo.onna.domain.external.BusanDistrict;
import com.nolleo.onna.domain.external.congestion.CongestionCacheService;
import com.nolleo.onna.domain.external.congestion.client.CongestionApiClient;
import com.nolleo.onna.domain.external.congestion.dto.CongestionAttractionInfo;
import com.nolleo.onna.domain.external.congestion.dto.CongestionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TourAPI 관광지 집중률 매일 00:00 갱신.
 * 구 평균 + 관광지별 집중률을 하나의 캐시 엔트리에 저장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CongestionScheduler {

    private final CongestionApiClient congestionApiClient;
    private final CongestionCacheService congestionCacheService;

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshAll() {
        log.info("혼잡도 캐시 갱신 시작");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (BusanDistrict district : BusanDistrict.values()) {
            try {
                List<Map<String, String>> items =
                        congestionApiClient.fetchCongestion(district.areaCd, district.signguCd);

                List<Map<String, String>> todayItems = items.stream()
                        .filter(item -> today.equals(item.get("baseYmd")))
                        .toList();

                if (todayItems.isEmpty()) {
                    log.warn("혼잡도 오늘 데이터 없음 district={}", district.districtName);
                    continue;
                }

                CongestionInfo info = buildCongestionInfo(district, todayItems, today);
                congestionCacheService.save(district.areaCd, district.signguCd, info);
                log.info("혼잡도 캐시 갱신 완료 district={} 관광지={}개",
                        district.districtName, info.attractions().size());

            } catch (Exception e) {
                log.error("혼잡도 캐시 갱신 실패 district={}: {}", district.districtName, e.getMessage());
            } finally {
                try { Thread.sleep(5000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("혼잡도 캐시 갱신 완료");
    }

    private CongestionInfo buildCongestionInfo(BusanDistrict district,
                                               List<Map<String, String>> todayItems,
                                               String today) {
        // 관광지별로 그룹핑 후 관광지 목록 생성
        List<CongestionAttractionInfo> attractions = todayItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrDefault("tAtsNm", "")))
                .entrySet().stream()
                .filter(e -> !e.getKey().isBlank())
                .map(e -> new CongestionAttractionInfo(
                        e.getKey(),
                        String.format("%.1f", avgRate(e.getValue()))
                ))
                .sorted((a, b) -> Double.compare(
                        Double.parseDouble(b.rate()), Double.parseDouble(a.rate())))
                .toList();

        // 구 전체 평균
        double districtAvg = attractions.stream()
                .mapToDouble(a -> parseRate(a.rate()))
                .average()
                .orElse(0);

        return new CongestionInfo(
                district.districtName,
                String.format("%.1f", districtAvg),
                today,
                attractions
        );
    }

    private double avgRate(List<Map<String, String>> items) {
        return items.stream()
                .mapToDouble(item -> parseRate(item.get("cnctrRate")))
                .average()
                .orElse(0);
    }

    private double parseRate(String value) {
        try { return Double.parseDouble(value); }
        catch (Exception e) { return 0; }
    }
}
