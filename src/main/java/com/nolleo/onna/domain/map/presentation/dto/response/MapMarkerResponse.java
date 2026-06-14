package com.nolleo.onna.domain.map.presentation.dto.response;

import com.nolleo.onna.domain.food.domain.model.FoodPlace;
import com.nolleo.onna.domain.food.domain.model.vo.GeoCoordinate;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "지도 마커 정보")
public record MapMarkerResponse(

        @Schema(
                description = "마커 타입 — SPOT(관광지), FOOD(음식점)",
                allowableValues = {"SPOT", "FOOD"},
                example = "SPOT"
        )
        MarkerType type,

        @Schema(
                description = "타입별 식별자. SPOT → contentId(관광공사 ID), FOOD → 내부 DB id. " +
                              "상세 조회 시 각각 GET /spots/{contentId}, GET /food/{id} 에 사용",
                example = "1234567890"
        )
        String id,

        @Schema(description = "장소명 또는 음식점명", example = "해운대 해수욕장")
        String title,

        @Schema(description = "경도 (WGS84)", example = "129.1603387")
        BigDecimal mapX,

        @Schema(description = "위도 (WGS84)", example = "35.1588473")
        BigDecimal mapY,

        @Schema(description = "대표 이미지 URL. FOOD 타입은 항상 null", example = "https://example.com/img.jpg")
        String firstImage,

        @Schema(description = "분류명. SPOT → 관광지 대분류(lclsSystm1), FOOD → 표준 업종 분류", example = "관광지")
        String category

) {
    public enum MarkerType { SPOT, FOOD }

    public static MapMarkerResponse fromSpot(Spot spot) {
        com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate geo = spot.getGeoCoordinate();
        return new MapMarkerResponse(
                MarkerType.SPOT,
                spot.getContentId(),
                spot.getTitle(),
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                spot.getFirstImage(),
                spot.getLclsSystm1()
        );
    }

    public static MapMarkerResponse fromFood(FoodPlace food) {
        GeoCoordinate geo = food.getGeoCoordinate();
        return new MapMarkerResponse(
                MarkerType.FOOD,
                String.valueOf(food.getId()),
                food.getName(),
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                null,
                food.getNormalizedCategory()
        );
    }
}