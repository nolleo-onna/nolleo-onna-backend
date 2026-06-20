package com.nolleo.onna.domain.map.presentation.dto.request;

import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지도 필터 조건")
public record MapFilterRequest(

        @Schema(description = "구군 필터 (예: 해운대구, 동구). null이면 전체", example = "해운대구")
        String district,

        @Schema(description = "카테고리 필터 (FD/VE/NA/HS/EX/LS). null이면 전체",
                allowableValues = {"FD", "VE", "NA", "HS", "EX", "LS"})
        PlaceCategory category,

        @Schema(description = "최대 예산 (원). null이면 제한 없음. 무료(is_free=true) 장소는 항상 포함",
                example = "20000")
        Integer maxBudget
) {}
