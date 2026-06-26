package com.nolleo.onna.domain.generatedcourse.presentation.dto.response;

import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourseItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 아이템 (방문 스팟)")
public record CourseItemResponse(

        @Schema(description = "방문 순서 (1부터 시작)", example = "1")
        Short serialNum,

        @Schema(description = "스팟 TourAPI content_id", example = "2760699")
        String spotContentId,

        @Schema(description = "스팟 이름", example = "광안리 해수욕장")
        String spotTitle,

        @Schema(description = "스팟 대표 이미지 URL", example = "https://...")
        String firstImage,

        @Schema(description = "이전 스팟으로부터 직선 거리 (미터), 첫 번째 스팟은 0", example = "350")
        Short distanceFromPrevM,

        @Schema(description = "예상 체류 시간 (분)", example = "80")
        Short durationMinutes,

        @Schema(description = "예상 방문 비용 (원) — 가격 데이터 확보 후 제공 예정", example = "null")
        Integer expectedCost,

        @Schema(description = "날씨 위험 경고 여부", example = "false")
        boolean hasWeatherWarning,

        @Schema(description = "날씨 위험 경고 메시지 (hasWeatherWarning=true 일 때만 존재)", example = "야외 공간입니다. 비 예보를 확인하세요.")
        String warningMessage
) {
    public static CourseItemResponse from(GeneratedCourseItem item) {
        return new CourseItemResponse(
                item.getSerialNum(),
                item.getSpotContentId(),
                null,   // spotTitle — 스팟 조회 후 채움 (Service 레이어에서 주입)
                null,   // firstImage — 스팟 조회 후 채움
                item.getDistanceFromPrevM(),
                item.getDurationMinutes(),
                item.getExpectedCost(),
                item.getWeatherWarning() != null && item.getWeatherWarning().hasWarning(),
                item.getWeatherWarning() != null ? item.getWeatherWarning().message() : null
        );
    }
}
