package com.nolleo.onna.domain.map.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구별 장소 수")
public record DistrictCountResponse(

        @Schema(description = "구군명", example = "해운대구")
        String district,

        @Schema(description = "장소 수", example = "42")
        long count

) {}
