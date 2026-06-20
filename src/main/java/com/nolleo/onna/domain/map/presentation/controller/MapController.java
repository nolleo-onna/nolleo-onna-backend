package com.nolleo.onna.domain.map.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.map.application.service.MapQueryService;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.presentation.dto.response.MapMarkerResponse;
import com.nolleo.onna.domain.map.presentation.dto.response.MapPlaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            description = "지도에 표시할 통합 마커 목록을 반환합니다. district/category/maxBudget 으로 필터링 가능합니다."
    )
    public ResponseEntity<ApiResponseDto<List<MapMarkerResponse>>> getMapMarkers(
            @Parameter(description = "구군 필터 (예: 해운대구). null이면 전체") @RequestParam(required = false) String district,
            @Parameter(description = "카테고리 코드 필터 (FD/VE/NA/HS/EX/LS). null이면 전체") @RequestParam(required = false) PlaceCategory category,
            @Parameter(description = "최대 예산(원). null이면 제한 없음") @RequestParam(required = false) Integer maxBudget
    ) {
        return ApiResponseDto.success(200, "지도 마커 목록 조회 성공",
                mapQueryService.getMapMarkers(district, category, maxBudget));
    }

    @GetMapping("/places")
    @Operation(
            summary = "지도 장소 목록 조회",
            description = "지도 장소 목록을 이미지·카테고리·가격 정보와 함께 반환합니다. district/category/maxBudget 으로 필터링 가능합니다."
    )
    public ResponseEntity<ApiResponseDto<Page<MapPlaceResponse>>> getMapPlaces(
            @Parameter(description = "구군 필터 (예: 해운대구). null이면 전체") @RequestParam(required = false) String district,
            @Parameter(description = "카테고리 코드 필터 (FD/VE/NA/HS/EX/LS). null이면 전체") @RequestParam(required = false) PlaceCategory category,
            @Parameter(description = "최대 예산(원). null이면 제한 없음") @RequestParam(required = false) Integer maxBudget,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ApiResponseDto.success(200, "지도 장소 목록 조회 성공",
                mapQueryService.getMapPlaces(district, category, maxBudget, pageable));
    }
}
