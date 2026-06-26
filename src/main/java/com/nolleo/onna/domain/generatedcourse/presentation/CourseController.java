package com.nolleo.onna.domain.generatedcourse.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.generatedcourse.presentation.dto.request.GenerateCourseRequest;
import com.nolleo.onna.domain.generatedcourse.presentation.dto.response.GenerateCourseResponse;
import com.nolleo.onna.domain.generatedcourse.presentation.dto.response.PairCourseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// WebConfig가 "/api/v1" 프리픽스를 자동 부여 → 실제 경로는 /api/v1/courses/**.
@Tag(name = "Course", description = "코스 생성/재생성/조회 API")
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    @Operation(
            summary = "코스 생성",
            description = "사용자 입력 조건(지역·예산·여행시간·동행유형)을 기반으로 AI가 2~3개의 코스를 생성합니다."
    )
    @PostMapping("/generate")
    public ResponseEntity<ApiResponseDto<GenerateCourseResponse>> generate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody GenerateCourseRequest request) {
        // TODO: CourseGenerationService 구현 후 연결
        throw new UnsupportedOperationException("구현 예정");
    }

    @Operation(
            summary = "코스 재생성",
            description = "기존 코스의 입력 조건을 그대로 사용하되, 기존 스팟을 제외하고 새 코스를 생성합니다."
    )
    @PostMapping("/{courseId}/regenerate")
    public ResponseEntity<ApiResponseDto<GenerateCourseResponse>> regenerate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "재생성 기준 코스 ID", example = "101")
            @PathVariable Long courseId) {
        // TODO: CourseGenerationService 구현 후 연결
        throw new UnsupportedOperationException("구현 예정");
    }

    @Operation(
            summary = "pairId로 코스 묶음 조회",
            description = "같은 입력 조건으로 생성된 코스 묶음(2~3개)을 조회합니다."
    )
    @GetMapping("/pair/{pairId}")
    public ResponseEntity<ApiResponseDto<PairCourseResponse>> getByPairId(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "코스 묶음 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID pairId) {
        // TODO: CourseGenerationService 구현 후 연결
        throw new UnsupportedOperationException("구현 예정");
    }
}
