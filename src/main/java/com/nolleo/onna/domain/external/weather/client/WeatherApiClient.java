package com.nolleo.onna.domain.external.weather.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 기상청 초단기실황 API 호출 클라이언트.
 * 매 정시 10분 이후 데이터 제공 → base_time은 현재 시각의 정시.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherApiClient {

    private static final String BASE_URL =
            "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";

    @Value("${external.weather.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchCurrentWeather(int nx, int ny) {
        LocalDateTime now = LocalDateTime.now();
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = String.format("%02d00", now.getHour());

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", apiKey)
                .queryParam("numOfRows", 1000)
                .queryParam("pageNo", 1)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(false)
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> body = (Map<String, Object>)
                    ((Map<String, Object>) response.get("response")).get("body");
            Map<String, Object> items = (Map<String, Object>) body.get("items");
            return (List<Map<String, Object>>) items.get("item");
        } catch (Exception e) {
            log.error("기상청 초단기실황 API 호출 실패 nx={} ny={}: {}", nx, ny, e.getMessage());
            throw new RuntimeException("기상청 API 호출 실패 nx=" + nx + " ny=" + ny, e);
        }
    }
}
