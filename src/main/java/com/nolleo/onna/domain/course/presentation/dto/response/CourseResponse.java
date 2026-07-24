package com.nolleo.onna.domain.course.presentation.dto.response;

import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "생성된 코스 상세")
public record CourseResponse(

        @Schema(description = "코스 ID", example = "1")
        Long id,

        @Schema(description = "같은 요청으로 생성된 형제 코스 묶음 UUID")
        UUID pairId,

        @Schema(description = "생성 방식", example = "AI")
        String generationMode,

        @Schema(description = "코스 제목", example = "광안리 로맨틱 데이트 코스")
        String title,

        @Schema(description = "코스 소개 문구")
        String description,

        @Schema(description = "예상 총 비용 (원) — 음식점 미포함 시 null")
        Integer totalCost,

        @Schema(description = "방문 스팟 목록 (방문 순서대로)")
        List<CourseItemResponse> items,

        @Schema(description = "생성 시각")
        OffsetDateTime createdAt

) {
    public static CourseResponse of(Course course, Map<String, Spot> spotByContentId) {
        List<CourseItemResponse> items = course.getItems().stream()
                .map(item -> CourseItemResponse.of(item, spotByContentId.get(item.getSpotContentId())))
                .toList();

        return new CourseResponse(
                course.getId(),
                course.getPairId(),
                course.getGenerationMode().name(),
                course.getTitle(),
                course.getDescription(),
                course.getTotalCost(),
                items,
                course.getCreatedAt()
        );
    }
}
