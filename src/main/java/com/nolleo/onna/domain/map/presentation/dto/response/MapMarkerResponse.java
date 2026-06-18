package com.nolleo.onna.domain.map.presentation.dto.response;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "지도 마커 정보")
public record MapMarkerResponse(

        @Schema(description = "장소 타입 — SPOT(관광지), FOOD(음식점)",
                allowableValues = {"SPOT", "FOOD"}, example = "SPOT")
        PlaceType type,

        @Schema(description = "타입별 식별자. SPOT → contentId, FOOD → 내부 DB id." +
                              " 상세 조회 시 각각 GET /spots/{contentId}, GET /food/{id} 에 사용",
                example = "1234567890")
        String id,

        @Schema(description = "장소명", example = "해운대 해수욕장")
        String title,

        @Schema(description = "경도 (WGS84)", example = "129.1603387")
        BigDecimal mapX,

        @Schema(description = "위도 (WGS84)", example = "35.1588473")
        BigDecimal mapY,

        @Schema(description = "대표 이미지 URL. FOOD 타입은 항상 null",
                example = "https://example.com/img.jpg")
        String firstImage,

        @Schema(description = "카테고리 코드 (FD/VE/NA/HS/EX/LS)", example = "NA")
        String category

) {
    public static MapMarkerResponse fromMapPlace(MapPlace mapPlace) {
        return new MapMarkerResponse(
                mapPlace.getPlaceType(),
                mapPlace.getOriginalId(),
                mapPlace.getName(),
                mapPlace.getLongitude(),
                mapPlace.getLatitude(),
                mapPlace.getImageUrl(),
                mapPlace.getCategory() != null ? mapPlace.getCategory().name() : null
        );
    }
}
