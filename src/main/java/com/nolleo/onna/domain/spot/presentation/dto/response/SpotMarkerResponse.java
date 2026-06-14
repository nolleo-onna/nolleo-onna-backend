package com.nolleo.onna.domain.spot.presentation.dto.response;

import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "지도 마커용 장소 요약 정보")
public record SpotMarkerResponse(

        @Schema(description = "한국관광공사 Tour API 콘텐츠 ID", example = "2760699")
        String contentId,

        @Schema(
                description = "콘텐츠 유형 코드. 12=관광지, 14=문화시설, 15=축제·공연·행사, 28=레포츠, 32=숙박, 38=쇼핑, 39=음식점",
                example = "12"
        )
        String contentTypeId,

        @Schema(description = "장소명", example = "비석문화마을")
        String title,

        @Schema(description = "경도 (WGS84)", example = "129.0124203")
        BigDecimal mapX,

        @Schema(description = "위도 (WGS84)", example = "35.0998088")
        BigDecimal mapY,

        @Schema(description = "대표 이미지 URL", example = "https://tong.visitkorea.or.kr/cms/resource/31/4068231_image2_1.jpg")
        String firstImage,

        @Schema(description = "서비스 분류 대분류 코드 (예: VE=자연, HS=역사, CT=문화관광, AC=레포츠)", example = "VE")
        String lclsSystm1,

        @Schema(description = "서비스 분류 중분류 코드", example = "VE04")
        String lclsSystm2,

        @Schema(description = "서비스 분류 소분류 코드", example = "VE040200")
        String lclsSystm3

) {
    public static SpotMarkerResponse from(Spot spot) {
        GeoCoordinate geo = spot.getGeoCoordinate();
        return new SpotMarkerResponse(
                spot.getContentId(),
                spot.getContentTypeId(),
                spot.getTitle(),
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                spot.getFirstImage(),
                spot.getLclsSystm1(),
                spot.getLclsSystm2(),
                spot.getLclsSystm3()
        );
    }
}