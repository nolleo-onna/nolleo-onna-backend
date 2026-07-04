package com.nolleo.onna.domain.external.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.external.congestion.CongestionQueryService;
import com.nolleo.onna.domain.external.congestion.dto.CongestionResponse;
import com.nolleo.onna.domain.external.congestion.scheduler.CongestionScheduler;
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
@RequestMapping("/congestion")
@RequiredArgsConstructor
@Tag(name = "Congestion", description = "혼잡도 조회 API")
public class CongestionController {

    private final CongestionQueryService congestionQueryService;
    private final CongestionScheduler congestionScheduler;

    @GetMapping
    @Operation(
            summary = "혼잡도 조회",
            description = "district 파라미터 없으면 부산 전체 지역 혼잡도 반환, 있으면 해당 지역만 반환. Redis 캐시 기준 (24시간 TTL)."
    )
    public ResponseEntity<ApiResponseDto<Object>> getCongestion(
            @Parameter(description = "구 이름 (예: 해운대구). null이면 전체") @RequestParam(required = false) String district
    ) {
        if (district == null) {
            List<CongestionResponse> result = congestionQueryService.getAllDistricts();
            return ApiResponseDto.success(200, "혼잡도 조회 성공", result);
        }
        CongestionResponse result = congestionQueryService.getByDistrict(district);
        return ApiResponseDto.success(200, "혼잡도 조회 성공", result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "[테스트용] 혼잡도 캐시 즉시 갱신", description = "스케줄러를 수동으로 즉시 실행합니다.")
    public ResponseEntity<ApiResponseDto<Object>> refreshCongestion() {
        congestionScheduler.refreshAll();
        return ApiResponseDto.success(200, "혼잡도 캐시 갱신 완료", null);
    }
}
