package com.nolleo.onna.domain.user.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.favorite.application.service.FavoriteQueryService;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 API")
public class UserController {

    private final FavoriteQueryService favoriteQueryService;

    @GetMapping("/me/favorite-stats")
    @Operation(
            summary = "마이페이지 찜 통계 조회",
            description = "오늘 → 이번 주 → 이번 달 순으로 우선순위가 높은 기간의 찜 통계를 반환합니다. 로그인이 필요합니다."
    )
    public ResponseEntity<ApiResponseDto<FavoriteStatsResponse>> getFavoriteStats(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        FavoriteStatsResponse response = favoriteQueryService.getStats(principal.userId());
        return ApiResponseDto.success(200, "찜 통계 조회 성공", response);
    }
}
