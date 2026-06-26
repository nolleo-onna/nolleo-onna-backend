package com.nolleo.onna.domain.generatedcourse.presentation.dto.response;

import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "생성된 코스")
public record CourseResponse(

        @Schema(description = "코스 ID", example = "101")
        Long id,

        @Schema(description = "재생성 시 원본 코스 ID (최초 생성이면 null)", example = "null")
        Long parentCourseId,

        @Schema(description = "생성 모드 (최초: GENERATED, 재생성: REGENERATED)", example = "GENERATED")
        String generationMode,

        @Schema(description = "코스 제목", example = "광안리 감성 반나절 코스")
        String title,

        @Schema(description = "코스 추천 이유 설명", example = "연인과 함께 걷기 좋은 해변에서 시작해...")
        String description,

        @Schema(description = "예상 총 비용 (원) — 가격 데이터 확보 후 제공 예정", example = "null")
        Integer totalCost,

        @Schema(description = "예상 총 소요 시간 (분)", example = "165")
        Integer totalMinutes,

        @Schema(description = "방문 스팟 목록 (순서대로)")
        List<CourseItemResponse> items
) {
    public static CourseResponse from(GeneratedCourse course) {
        return new CourseResponse(
                course.getId(),
                course.getParentCourseId(),
                course.getGenerationMode(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseSummary() != null ? course.getCourseSummary().totalCost() : null,
                course.getCourseSummary() != null ? course.getCourseSummary().totalMinutes() : null,
                course.getItems().stream().map(CourseItemResponse::from).toList()
        );
    }
}
