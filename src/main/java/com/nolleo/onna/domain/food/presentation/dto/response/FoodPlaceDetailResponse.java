package com.nolleo.onna.domain.food.presentation.dto.response;

import com.nolleo.onna.domain.food.domain.model.FoodPlace;
import com.nolleo.onna.domain.food.domain.model.FoodPlaceMenu;
import com.nolleo.onna.domain.food.domain.model.vo.GeoCoordinate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "음식점 상세 정보")
public record FoodPlaceDetailResponse(

        @Schema(description = "내부 ID", example = "1")
        Long id,

        @Schema(description = "업소명", example = "해운대 암소갈비집")
        String name,

        @Schema(description = "서비스 표준 업종 분류", example = "FD")
        String normalizedCategory,

        @Schema(description = "주소", example = "부산광역시 해운대구 중동 1519-2")
        String address,

        @Schema(description = "전화번호", example = "051-742-5533")
        String tel,

        @Schema(description = "업소 소개", example = "1984년부터 이어온 부산 대표 암소갈비 전문점")
        String description,

        @Schema(description = "대표 메뉴", example = "암소갈비")
        String representativeMenu,

        @Schema(description = "영업시간 (비정형 텍스트)", example = "11:00~22:00")
        String businessHoursRaw,

        @Schema(description = "배달 가능 여부. null=정보 없음", example = "false")
        Boolean deliveryAvailable,

        @Schema(description = "주차 가능 여부. null=정보 없음", example = "true")
        Boolean parkingAvailable,

        @Schema(description = "구군명", example = "해운대구")
        String district,

        @Schema(description = "경도 (WGS84)", example = "129.1603387")
        BigDecimal mapX,

        @Schema(description = "위도 (WGS84)", example = "35.1588473")
        BigDecimal mapY,

        @Schema(description = "메뉴 목록")
        List<MenuInfo> menus

) {
    @Schema(description = "메뉴 정보")
    public record MenuInfo(
            @Schema(description = "메뉴명", example = "암소갈비")
            String menuName,

            @Schema(description = "가격 (원). null=가격 정보 없음", example = "35000")
            Integer price,

            @Schema(description = "대표 메뉴 여부", example = "true")
            boolean representative
    ) {
        public static MenuInfo from(FoodPlaceMenu menu) {
            return new MenuInfo(
                    menu.getMenuName(),
                    menu.getMoney() != null ? menu.getMoney().price() : null,
                    menu.isRepresentative()
            );
        }
    }

    public static FoodPlaceDetailResponse from(FoodPlace food) {
        GeoCoordinate geo = food.getGeoCoordinate();
        return new FoodPlaceDetailResponse(
                food.getId(),
                food.getName(),
                food.getNormalizedCategory(),
                food.getAddress(),
                food.getTel(),
                food.getDescription(),
                food.getRepresentativeMenu(),
                food.getBusinessHoursRaw(),
                food.getDeliveryAvailable(),
                food.getParkingAvailable(),
                food.getSourceRegion(),
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                food.getMenus().stream().map(MenuInfo::from).toList()
        );
    }
}
