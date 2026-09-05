package com.nolleo.onna.domain.external.weather;

import com.nolleo.onna.domain.external.BusanDistrict;
import com.nolleo.onna.domain.external.weather.client.WeatherApiClient;
import com.nolleo.onna.domain.external.weather.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherRefreshService {

    private final WeatherApiClient weatherApiClient;
    private final WeatherCacheService weatherCacheService;

    public void refresh() {
        log.info("날씨 실황 캐시 갱신 시작");
        List<BusanDistrict> failed = fetchAll(List.of(BusanDistrict.values()));

        if (!failed.isEmpty()) {
            log.info("날씨 캐시 갱신 실패 지역 재시도: {}개", failed.size());
            sleepQuietly(5000);
            List<BusanDistrict> stillFailed = fetchAll(failed);
            if (!stillFailed.isEmpty()) {
                log.warn("날씨 캐시 갱신 최종 실패 지역: {}",
                        stillFailed.stream().map(d -> d.districtName).toList());
            }
        }
        log.info("날씨 실황 캐시 갱신 완료");
    }

    private List<BusanDistrict> fetchAll(List<BusanDistrict> districts) {
        List<BusanDistrict> failed = new ArrayList<>();
        for (BusanDistrict district : districts) {
            try {
                List<Map<String, Object>> items =
                        weatherApiClient.fetchCurrentWeather(district.nx, district.ny);
                WeatherInfo info = parse(items);
                weatherCacheService.save(district.districtName, info);
                log.info("날씨 실황 캐시 갱신 완료 district={}", district.districtName);
            } catch (Exception e) {
                log.error("날씨 실황 캐시 갱신 실패 district={}: {}", district.districtName, e.getMessage());
                failed.add(district);
            } finally {
                sleepQuietly(5000);
            }
        }
        return failed;
    }

    private WeatherInfo parse(List<Map<String, Object>> items) {
        String tmp = "-", pty = "0", reh = "-", wsd = "-", rn1 = "0";
        for (Map<String, Object> item : items) {
            String category = String.valueOf(item.get("category"));
            String value    = String.valueOf(item.get("obsrValue"));
            if (isMissing(value)) continue;
            switch (category) {
                case "T1H" -> tmp = value;
                case "PTY" -> pty = value;
                case "REH" -> reh = value;
                case "WSD" -> wsd = value;
                case "RN1" -> rn1 = value;
            }
        }
        return new WeatherInfo(tmp, pty, reh, wsd, rn1);
    }

    private boolean isMissing(String value) {
        try {
            return Double.parseDouble(value) < -900;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
