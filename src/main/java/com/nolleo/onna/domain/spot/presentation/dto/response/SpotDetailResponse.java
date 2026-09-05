package com.nolleo.onna.domain.spot.presentation.dto.response;

import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.SpotDetail;
import com.nolleo.onna.domain.spot.domain.model.SpotImage;
import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "장소 상세 정보")
public record SpotDetailResponse(

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

        @Schema(description = "대표 이미지 썸네일 URL", example = "https://tong.visitkorea.or.kr/cms/resource/31/4068231_image3_1.jpg")
        String firstImage2,

        @Schema(description = "서비스 분류 대분류 코드 (예: VE=자연, HS=역사, CT=문화관광, AC=레포츠)", example = "VE")
        String lclsSystm1,

        @Schema(description = "서비스 분류 중분류 코드", example = "VE04")
        String lclsSystm2,

        @Schema(description = "서비스 분류 소분류 코드", example = "VE040200")
        String lclsSystm3,

        @Schema(description = "법정동 시도 코드 (26=부산광역시)", example = "26")
        String lDongRegnCd,

        @Schema(description = "법정동 시군구 코드 (140=서구, 170=동구 등)", example = "140")
        String lDongSignguCd,

        @Schema(description = "전화번호", example = "051-240-6541")
        String tel,

        @Schema(description = "홈페이지 URL", example = "https://www.bsseogu.go.kr/tour/")
        String homepage,

        @Schema(description = "주소 (도로명 또는 지번)", example = "부산광역시 서구 아미로 49 (아미동2가)")
        String addr1,

        @Schema(description = "상세 주소 (건물명, 동호수 등)", example = "3층")
        String addr2,

        @Schema(description = "우편번호", example = "49243")
        String zipcode,

        @Schema(description = "장소 소개글", example = "아미동 비석문화마을은 부산의 역사를 좀 더 단적으로 보여주는 동네이다...")
        String overview,

        @Schema(
                description = "부가 정보 JSON 문자열. parking(주차), usetime(이용시간), restdate(휴무일), " +
                              "infocenter(문의처), chkbabycarriage(유모차 가능 여부), chkpet(반려동물 가능 여부) 등 포함. " +
                              "콘텐츠 유형별로 제공 항목이 다를 수 있음",
                example = "{\"parking\":\"가능요금 (10분 당 100원)\",\"usetime\":\"상시 개방\",\"restdate\":\"연중무휴\"}"
        )
        String intro,

        @Schema(description = "주차 가능 여부. null=정보 없음", example = "true")
        Boolean parkingAvailable,

        @Schema(description = "추가 이미지 목록")
        List<ImageInfo> images,

        @Schema(description = "최저 가격 (원). 가격 정보 없으면 null", example = "8000")
        Integer minPrice,

        @Schema(description = "평균 가격 (원). 가격 정보 없으면 null", example = "34975")
        Integer avgPrice,

        @Schema(description = "대표 메뉴명. 가격 정보 없으면 null", example = "유니짜장")
        String representativeMenuName,

        @Schema(description = "대표 메뉴 가격 (원). 가격 정보 없으면 null", example = "8000")
        Integer representativePrice

) {
    @Schema(description = "장소 이미지")
    public record ImageInfo(
            @Schema(description = "원본 이미지 URL", example = "https://tong.visitkorea.or.kr/cms/resource/97/3498697_image2_1.jpg")
            String originImgUrl,

            @Schema(description = "썸네일 이미지 URL", example = "https://tong.visitkorea.or.kr/cms/resource/97/3498697_image2_1.jpg")
            String smallImgUrl,

            @Schema(description = "이미지 파일명", example = "부산_비석문화마을 (14)")
            String imgName
    ) {
        public static ImageInfo from(SpotImage image) {
            return new ImageInfo(image.getOriginImgUrl(), image.getSmallImgUrl(), image.getImgName());
        }
    }

    public static SpotDetailResponse of(
            Spot spot,
            SpotDetail detail,
            List<SpotImage> images,
            SpotPriceSummary priceSummary
    ) {
        GeoCoordinate geo = spot.getGeoCoordinate();
        return new SpotDetailResponse(
                spot.getContentId(),
                spot.getContentTypeId(),
                spot.getTitle(),
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null,
                spot.getFirstImage(),
                spot.getFirstImage2(),
                spot.getLclsSystm1(),
                spot.getLclsSystm2(),
                spot.getLclsSystm3(),
                spot.getLDongRegnCd(),
                spot.getLDongSignguCd(),
                detail != null ? detail.getTel() : null,
                detail != null ? detail.getHomepage() : null,
                detail != null ? detail.getAddr1() : null,
                detail != null ? detail.getAddr2() : null,
                detail != null ? detail.getZipcode() : null,
                detail != null ? detail.getOverview() : null,
                detail != null ? detail.getIntro() : null,
                detail != null ? detail.getParkingAvailable() : null,
                images.stream().map(ImageInfo::from).toList(),
                priceSummary != null ? priceSummary.getMinPrice() : null,
                priceSummary != null ? priceSummary.getAvgPrice() : null,
                priceSummary != null ? priceSummary.getRepresentativeMenuName() : null,
                priceSummary != null ? priceSummary.getRepresentativePrice() : null
        );
    }
}