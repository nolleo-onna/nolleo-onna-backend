package com.nolleo.onna.domain.review.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.review.application.service.ReviewCommandService;
import com.nolleo.onna.domain.review.presentation.dto.request.CreateReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "리뷰 API")
public class ReviewController {

    private final ReviewCommandService reviewCommandService;

    @PostMapping
    @Operation(
            summary = "별점 리뷰 등록",
            description = "MapPlace에 1~5점 별점을 등록합니다. 로그인이 필요합니다."
    )
    public ResponseEntity<ApiResponseDto<Void>> createReview(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        reviewCommandService.createReview(principal.userId(), request.mapPlaceId(), request.rating());
        return ApiResponseDto.success(201, "리뷰 등록 성공", null);
    }
}