package com.nolleo.onna.domain.generatedcourse.presentation.dto.response;

import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourseItem;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "코스 아이템 (방문 장소)")
public record CourseItemResponse(

        @Schema(description = "방문 순서 (1부터 시작)", example = "1")
        Short serialNum,

        @Schema(description = "장소 타입 — SPOT(관광지), FOOD(음식점)", example = "SPOT")
        PlaceType placeType,

        @Schema(description = "카테고리 코드 (FD/VE/NA/HS/EX/LS)", example = "NA")
        PlaceCategory category,

        @Schema(description = "장소명", example = "광안리 해수욕장")
        String name,

        @Schema(description = "대표 이미지 URL", example = "https://...")
        String imageUrl,

        @Schema(description = "경도 (WGS84)", example = "129.1187")
        BigDecimal longitude,

        @Schema(description = "위도 (WGS84)", example = "35.1531")
        BigDecimal latitude,

        @Schema(description = "상세 조회용 원본 ID. SPOT → GET /spots/{id}, FOOD → GET /food/{id}", example = "2760699")
        String originalId,

        @Schema(description = "이전 장소로부터 직선 거리 (미터). 첫 번째 장소는 출발 지역 중심에서의 거리", example = "350")
        Short distanceFromPrevM,

        @Schema(description = "예상 체류 시간 (분)", example = "60")
        Short durationMinutes,

        @Schema(description = "예상 방문 비용 (원) — 가격 데이터 확보 후 제공 예정")
        Integer expectedCost

) {
    public static CourseItemResponse from(GeneratedCourseItem item, MapPlace place) {
        return new CourseItemResponse(
                item.getSerialNum(),
                place != null ? place.getPlaceType() : null,
                place != null ? place.getCategory() : null,
                place != null ? place.getName() : null,
                place != null ? place.getImageUrl() : null,
                place != null ? place.getLongitude() : null,
                place != null ? place.getLatitude() : null,
                place != null ? place.getOriginalId() : null,
                item.getDistanceFromPrevM(),
                item.getDurationMinutes(),
                item.getExpectedCost()
        );
    }
}
