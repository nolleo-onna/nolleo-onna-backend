package com.nolleo.onna.domain.generatedcourse.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.generatedcourse.application.dto.GenerateCourseCommand;
import com.nolleo.onna.domain.generatedcourse.application.service.CourseGenerationService;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseType;
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

@Tag(name = "Course", description = "코스 생성/재생성/조회 API")
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseGenerationService courseGenerationService;

    @Operation(
            summary = "코스 생성",
            description = "지역·여행시간·동행유형을 기반으로 ACTIVE/CULTURE/FOOD_TOUR 3가지 코스를 생성합니다."
    )
    @PostMapping("/generate")
    public ResponseEntity<ApiResponseDto<GenerateCourseResponse>> generate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody GenerateCourseRequest request) {
        Long userId = principal.userId();
        return ApiResponseDto.success(200, "코스 생성 성공",
                courseGenerationService.generate(GenerateCourseCommand.of(userId, request)));
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
        // TODO: 재생성 구현
        throw new UnsupportedOperationException("구현 예정");
    }

    @Operation(
            summary = "pairId로 코스 묶음 조회",
            description = "같은 입력 조건으로 생성된 코스 묶음을 조회합니다. type 파라미터로 특정 타입만 필터링 가능합니다."
    )
    @GetMapping("/pair/{pairId}")
    public ResponseEntity<ApiResponseDto<PairCourseResponse>> getByPairId(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "코스 묶음 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID pairId,
            @Parameter(description = "코스 타입 필터 (ACTIVE / CULTURE / FOOD_TOUR), 생략 시 전체 반환")
            @RequestParam(required = false) CourseType type) {
        return ApiResponseDto.success(200, "코스 조회 성공",
                courseGenerationService.getByPairId(pairId, type));
    }
}
