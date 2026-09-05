package com.nolleo.onna.domain.external.weather;

import com.nolleo.onna.domain.external.BusanDistrict;
import com.nolleo.onna.domain.external.weather.dto.WeatherInfo;
import com.nolleo.onna.domain.external.weather.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherQueryService {

    private final WeatherCacheService weatherCacheService;

    public List<WeatherResponse> getAllDistricts() {
        return Arrays.stream(BusanDistrict.values())
                .map(d -> {
                    WeatherInfo info = weatherCacheService.get(d.districtName)
                            .orElse(WeatherInfo.empty());
                    return WeatherResponse.of(d.districtName, info);
                })
                .distinct()
                .toList();
    }

    public WeatherResponse getByDistrict(String districtName) {
        WeatherInfo info = weatherCacheService.get(districtName)
                .orElse(WeatherInfo.empty());
        return WeatherResponse.of(districtName, info);
    }
}
