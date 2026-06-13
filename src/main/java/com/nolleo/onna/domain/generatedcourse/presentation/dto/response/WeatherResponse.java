package com.nolleo.onna.domain.generatedcourse.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 생성 시점 날씨 정보")
public record WeatherResponse(

        @Schema(description = "날씨 상태", example = "맑음")
        String condition,

        @Schema(description = "기온 (°C)", example = "24")
        Integer temperature,

        @Schema(description = "강수량 (mm)", example = "0")
        Integer precipitation
) {}
