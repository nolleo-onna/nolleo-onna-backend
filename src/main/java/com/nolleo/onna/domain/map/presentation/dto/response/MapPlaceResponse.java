package com.nolleo.onna.domain.map.presentation.dto.response;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "지도 장소 목록 아이템")
public record MapPlaceResponse(

        @Schema(description = "map_place 내부 ID", example = "1")
        Long id,

        @Schema(description = "장소 타입 — SPOT(관광지), FOOD(음식점)",
                allowableValues = {"SPOT", "FOOD"})
        PlaceType placeType,

        @Schema(description = "원본 테이블 ID. SPOT → contentId, FOOD → 내부 id", example = "1234567890")
        String originalId,

        @Schema(description = "장소명", example = "해운대 해수욕장")
        String name,

        @Schema(description = "구군명", example = "해운대구")
        String district,

        @Schema(description = "카테고리 코드", example = "NA",
                allowableValues = {"FD", "VE", "NA", "HS", "EX", "LS"})
        PlaceCategory category,

        @Schema(description = "경도 (WGS84)", example = "129.1603387")
        BigDecimal longitude,

        @Schema(description = "위도 (WGS84)", example = "35.1588473")
        BigDecimal latitude,

        @Schema(description = "대표 이미지 URL. FOOD 타입은 항상 null", example = "https://example.com/img.jpg")
        String imageUrl,

        @Schema(description = "최저 가격(원). null이면 가격 정보 없음", example = "10000")
        Integer minPrice,

        @Schema(description = "무료 여부", example = "false")
        boolean free

) {
    public static MapPlaceResponse from(MapPlace mapPlace) {
        return new MapPlaceResponse(
                mapPlace.getId(),
                mapPlace.getPlaceType(),
                mapPlace.getOriginalId(),
                mapPlace.getName(),
                mapPlace.getDistrict(),
                mapPlace.getCategory(),
                mapPlace.getLongitude(),
                mapPlace.getLatitude(),
                mapPlace.getImageUrl(),
                mapPlace.getMinPrice(),
                mapPlace.isFree()
        );
    }
}
