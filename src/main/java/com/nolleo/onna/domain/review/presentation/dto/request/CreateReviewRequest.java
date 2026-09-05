package com.nolleo.onna.domain.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(

        @NotNull(message = "mapPlaceId는 필수입니다.")
        Long mapPlaceId,

        @NotNull(message = "rating은 필수입니다.")
        @Min(value = 1, message = "rating은 최소 1점입니다.")
        @Max(value = 5, message = "rating은 최대 5점입니다.")
        Integer rating
) {}