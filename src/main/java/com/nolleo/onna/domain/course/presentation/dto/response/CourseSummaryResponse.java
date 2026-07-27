package com.nolleo.onna.domain.course.presentation.dto.response;

import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "코스 목록용 요약 정보 — 상세는 pairId로 GET /courses/{pairId} 조회")
public record CourseSummaryResponse(

        @Schema(description = "코스 ID", example = "1")
        Long id,

        @Schema(description = "같은 요청으로 생성된 형제 코스 묶음 UUID — 상세 조회에 사용", example = "9c2e4b1a-5f3d-4a2b-8e1c-7d6f5a4b3c2d")
        UUID pairId,

        @Schema(description = "코스 제목", example = "광안리 로맨틱 데이트 코스")
        String title,

        @Schema(description = "코스 소개 문구")
        String description,

        @Schema(description = "예상 총 비용 (원) — 음식점 미포함 시 null")
        Integer totalCost,

        @Schema(description = "방문 순서대로의 스팟 이름 목록", example = "[\"광안리해수욕장\", \"OO카페\"]")
        List<String> spotTitles

) {
    public static CourseSummaryResponse of(Course course, Map<String, Spot> spotByContentId) {
        List<String> spotTitles = course.getItems().stream()
                .map(item -> {
                    Spot spot = spotByContentId.get(item.getSpotContentId());
                    return spot != null ? spot.getTitle() : null;
                })
                .toList();

        return new CourseSummaryResponse(
                course.getId(),
                course.getPairId(),
                course.getTitle(),
                course.getDescription(),
                course.getTotalCost(),
                spotTitles
        );
    }
}
