package com.nolleo.onna.domain.external.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.external.weather.WeatherQueryService;
import com.nolleo.onna.domain.external.weather.WeatherRefreshService;
import com.nolleo.onna.domain.external.weather.dto.WeatherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
@Tag(name = "Weather", description = "날씨 조회 API")
public class WeatherController {

    private final WeatherQueryService weatherQueryService;
    private final WeatherRefreshService weatherRefreshService;

    @GetMapping
    @Operation(
            summary = "날씨 조회",
            description = "district 파라미터 없으면 부산 전체 지역 날씨 반환, 있으면 해당 지역만 반환. Redis 캐시 기준 (1시간 TTL)."
    )
    public ResponseEntity<ApiResponseDto<Object>> getWeather(
            @Parameter(description = "구 이름 (예: 해운대구). null이면 전체") @RequestParam(required = false) String district
    ) {
        if (district == null) {
            List<WeatherResponse> result = weatherQueryService.getAllDistricts();
            return ApiResponseDto.success(200, "날씨 조회 성공", result);
        }
        WeatherResponse result = weatherQueryService.getByDistrict(district);
        return ApiResponseDto.success(200, "날씨 조회 성공", result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "[테스트용] 날씨 캐시 즉시 갱신", description = "스케줄러를 수동으로 즉시 실행합니다.")
    public ResponseEntity<ApiResponseDto<Object>> refreshWeather() {
        weatherRefreshService.refresh();
        return ApiResponseDto.success(200, "날씨 캐시 갱신 완료", null);
    }
}
