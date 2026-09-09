package com.nolleo.onna.domain.course.application.dto.response;

import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "코스에 포함된 방문 스팟")
public record CourseItemResponse(

        @Schema(description = "방문 순서 (1부터 시작)", example = "1")
        Short serialNum,

        @Schema(description = "장소 타입 — SPOT(관광공사 스팟), FOOD(착한가게)", example = "SPOT")
        String placeType,

        @Schema(description = "원본 식별자 — SPOT → sp_spots.content_id, FOOD → fd_food_places.id", example = "2760699")
        String originalId,

        @Schema(description = "스팟명", example = "광안리해수욕장")
        String title,

        @Schema(description = "경도 (WGS84)", example = "129.1187")
        BigDecimal mapX,

        @Schema(description = "위도 (WGS84)", example = "35.1531")
        BigDecimal mapY,

        @Schema(description = "대표 이미지 URL")
        String firstImage,

        @Schema(description = "카테고리 (한국어)", example = "자연/공원")
        String category,

        @Schema(description = "예상 방문 비용 (원) — 음식점만, 그 외 null")
        Integer expectedCost,

        @Schema(description = "이전 장소로부터 직선 거리 (미터)", example = "850")
        Integer distanceFromPrevM

) {
    public static CourseItemResponse of(CourseItem item, SpotCandidate spot) {
        return new CourseItemResponse(
                item.getSerialNum(),
                item.getPlaceRef().type().name(),
                item.getPlaceRef().originalId(),
                spot != null ? spot.title() : null,
                spot != null ? spot.mapX() : null,
                spot != null ? spot.mapY() : null,
                spot != null ? spot.firstImage() : null,
                spot != null ? spot.categoryLabel() : null,
                item.getExpectedCost(),
                item.getDistanceFromPrevM()
        );
    }
}
