package com.nolleo.onna.domain.favorite.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.favorite.application.service.FavoriteCommandService;
import com.nolleo.onna.domain.favorite.application.service.FavoriteQueryService;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteItemResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatusResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteToggleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorite", description = "찜 API")
public class FavoriteController {

    private final FavoriteCommandService favoriteCommandService;
    private final FavoriteQueryService favoriteQueryService;

    @PostMapping("/{mapPlaceId}/toggle")
    @Operation(
            summary = "장소 찜 토글",
            description = "장소를 찜하거나 찜을 취소합니다. 로그인이 필요합니다."
    )
    public ResponseEntity<ApiResponseDto<FavoriteToggleResponse>> toggle(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "MapPlace ID") @PathVariable Long mapPlaceId
    ) {
        FavoriteToggleResponse response = favoriteCommandService.toggle(principal.userId(), mapPlaceId);
        String message = response.favorited() ? "찜 추가 성공" : "찜 취소 성공";
        return ApiResponseDto.success(200, message, response);
    }

    @GetMapping
    @Operation(
            summary = "찜한 장소 목록 조회",
            description = "로그인한 사용자의 찜한 장소 목록을 최신순으로 조회합니다. 로그인이 필요합니다."
    )
    public ResponseEntity<ApiResponseDto<Page<FavoriteItemResponse>>> getFavorites(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FavoriteItemResponse> response = favoriteQueryService.getFavorites(principal.userId(), pageable);
        return ApiResponseDto.success(200, "찜 목록 조회 성공", response);
    }

    @GetMapping("/{mapPlaceId}/status")
    @Operation(
            summary = "특정 장소 찜 여부 조회",
            description = "로그인한 사용자가 특정 장소를 찜했는지 확인합니다. 로그인이 필요합니다."
    )
    public ResponseEntity<ApiResponseDto<FavoriteStatusResponse>> getStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "MapPlace ID") @PathVariable Long mapPlaceId
    ) {
        FavoriteStatusResponse response = favoriteQueryService.getStatus(principal.userId(), mapPlaceId);
        return ApiResponseDto.success(200, "찜 여부 조회 성공", response);
    }
}
