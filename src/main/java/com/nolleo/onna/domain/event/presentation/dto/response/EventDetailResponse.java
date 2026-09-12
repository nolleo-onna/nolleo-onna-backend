package com.nolleo.onna.domain.event.presentation.dto.response;

import com.nolleo.onna.domain.event.application.dto.EventDetailResult;
import com.nolleo.onna.domain.event.domain.model.Event;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "행사 상세 정보")
public record EventDetailResponse(

        @Schema(description = "한국관광공사 Tour API 콘텐츠 ID", example = "2991394")
        String contentId,

        @Schema(description = "행사명", example = "2026 부산나이트워크42K with dsec")
        String title,

        @Schema(description = "행사 시작일", example = "2026-08-29")
        LocalDate eventStartDate,

        @Schema(description = "행사 종료일", example = "2026-08-30")
        LocalDate eventEndDate,

        @Schema(description = "경도 (WGS84)", example = "129.1273173")
        BigDecimal mapX,

        @Schema(description = "위도 (WGS84)", example = "35.1679082")
        BigDecimal mapY,

        @Schema(description = "전화번호", example = "070-4705-2008")
        String tel,

        @Schema(description = "주소", example = "부산광역시 해운대구 수영강변대로 85 (우동)")
        String addr1,

        @Schema(description = "상세 주소", example = "3층")
        String addr2,

        @Schema(description = "대표 이미지 URL", example = "https://tong.visitkorea.or.kr/cms/resource/37/4069137_image2_1.jpg")
        String firstImage,

        @Schema(description = "대표 이미지 썸네일 URL", example = "https://tong.visitkorea.or.kr/cms/resource/37/4069137_image3_1.jpg")
        String firstImage2,

        @Schema(description = "행사 장소명", example = "APEC나루공원")
        String eventPlace,

        @Schema(description = "운영 시간", example = "16:00~07:00")
        String playTime,

        @Schema(description = "이용 요금", example = "8K 38,000원 / 16K 42,000원")
        String useTimeFestival,

        @Schema(description = "주최자", example = "부산일보사, 어반씨앤에스")
        String sponsor1,

        @Schema(description = "주최자 연락처", example = "070-4705-2008")
        String sponsor1Tel,

        @Schema(description = "주관사", example = "㈜블렌트")
        String sponsor2,

        @Schema(description = "주관사 연락처", example = "070-4705-2008")
        String sponsor2Tel,

        @Schema(description = "관람 연령 제한", example = "만 16세 이상")
        String ageLimit,

        @Schema(description = "행사 홈페이지 URL", example = "https://example.com")
        String eventHomepage

) {
    public static EventDetailResponse from(EventDetailResult result) {
        Event event = result.event();
        return new EventDetailResponse(
                event.getContentId(),
                event.getTitle(),
                event.getEventStartDate(),
                event.getEventEndDate(),
                event.getMapX(),
                event.getMapY(),
                event.getTel(),
                event.getAddr1(),
                event.getAddr2(),
                event.getFirstImage(),
                event.getFirstImage2(),
                event.getEventPlace(),
                event.getPlayTime(),
                event.getUseTimeFestival(),
                event.getSponsor1(),
                event.getSponsor1Tel(),
                event.getSponsor2(),
                event.getSponsor2Tel(),
                event.getAgeLimit(),
                event.getEventHomepage()
        );
    }
}
