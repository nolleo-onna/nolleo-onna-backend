package com.nolleo.onna.domain.spot.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.spot.application.service.SpotQueryService;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotDetailResponse;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotMarkerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/spots")
@RequiredArgsConstructor
@Tag(name = "Spot", description = "장소 API")
public class SpotController {

    private final SpotQueryService spotQueryService;

    @GetMapping("/markers")
    @Operation(summary = "지도 마커 목록 조회", description = "지도에 표시할 활성 장소 전체 목록을 반환합니다.")
    public ResponseEntity<ApiResponseDto<List<SpotMarkerResponse>>> getSpotMarkers() {
        List<SpotMarkerResponse> data = spotQueryService.getSpotMarkers();
        return ApiResponseDto.success(200, "장소 목록 조회 성공", data);
    }

    @GetMapping("/{contentId}")
    @Operation(summary = "장소 상세 조회", description = "contentId로 장소 상세 정보를 반환합니다.")
    public ResponseEntity<ApiResponseDto<SpotDetailResponse>> getSpotDetail(
            @PathVariable String contentId) {
        SpotDetailResponse data = spotQueryService.getSpotDetail(contentId);
        return ApiResponseDto.success(200, "장소 상세 조회 성공", data);
    }
}
