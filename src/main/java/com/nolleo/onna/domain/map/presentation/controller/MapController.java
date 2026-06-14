package com.nolleo.onna.domain.map.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.map.application.service.MapQueryService;
import com.nolleo.onna.domain.map.presentation.dto.response.MapMarkerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
@Tag(name = "Map", description = "지도 API")
public class MapController {

    private final MapQueryService mapQueryService;

    @GetMapping("/markers")
    @Operation(
            summary = "지도 마커 목록 조회",
            description = "지도에 표시할 장소(SPOT)와 음식점(FOOD) 마커 전체 목록을 반환합니다. " +
                          "각 마커는 type 필드로 구분합니다."
    )
    public ResponseEntity<ApiResponseDto<List<MapMarkerResponse>>> getMapMarkers() {
        return ApiResponseDto.success(200, "지도 마커 목록 조회 성공", mapQueryService.getMapMarkers());
    }
}