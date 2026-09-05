package com.nolleo.onna.domain.food.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FoodErrorCode implements ErrorCode {

    FOOD_NOT_FOUND(404, "FOOD_NOT_FOUND", "음식점을 찾을 수 없습니다");

    private final int status;
    private final String errorCode;
    private final String message;
}
