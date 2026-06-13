package com.nolleo.onna.domain.spot.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SpotErrorCode implements ErrorCode {

    SPOT_NOT_FOUND(404, "SPOT_NOT_FOUND", "장소를 찾을 수 없습니다");

    private final int status;
    private final String errorCode;
    private final String message;
}
