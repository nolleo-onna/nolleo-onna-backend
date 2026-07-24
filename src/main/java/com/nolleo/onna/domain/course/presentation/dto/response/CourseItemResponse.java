package com.nolleo.onna.domain.course.presentation.dto.response;

import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;
import com.nolleo.onna.domain.spot.domain.model.vo.SpotCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "코스에 포함된 방문 스팟")
public record CourseItemResponse(

        @Schema(description = "방문 순서 (1부터 시작)", example = "1")
        Short serialNum,

        @Schema(description = "스팟 콘텐츠 ID", example = "2760699")
        String spotContentId,

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
        Short distanceFromPrevM

) {
    public static CourseItemResponse of(CourseItem item, Spot spot) {
        GeoCoordinate geo = spot != null ? spot.getGeoCoordinate() : null;
        SpotCategory category = spot != null ? spot.getCategory() : null;
        return new CourseItemResponse(
                item.getSerialNum(),
                item.getSpotContentId(),
                spot != null ? spot.getTitle() : null,
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                spot != null ? spot.getFirstImage() : null,
                category != null ? category.getLabel() : null,
                item.getExpectedCost(),
                item.getDistanceFromPrevM()
        );
    }
}
