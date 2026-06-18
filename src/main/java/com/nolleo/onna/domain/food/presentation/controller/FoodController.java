package com.nolleo.onna.domain.food.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.food.application.service.FoodQueryService;
import com.nolleo.onna.domain.food.presentation.dto.response.FoodPlaceDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
@Tag(name = "Food", description = "음식점 API")
public class FoodController {

    private final FoodQueryService foodQueryService;

    @GetMapping("/{id}")
    @Operation(summary = "음식점 상세 조회", description = "id로 음식점 상세 정보 및 메뉴 목록을 반환합니다.")
    public ResponseEntity<ApiResponseDto<FoodPlaceDetailResponse>> getFoodPlaceDetail(
            @PathVariable Long id) {
        return ApiResponseDto.success(200, "음식점 상세 조회 성공",
                foodQueryService.getFoodPlaceDetail(id));
    }
}
