// [Common] 비즈니스 규칙 위반 예외 — 도메인·서비스 레이어에서 ErrorCode를 담아 던지는 런타임 예외.
// throw new BusinessException(UserErrorCode.USER_NOT_FOUND) 형태로 사용하며 GlobalExceptionHandler에서 일괄 처리.
package com.nolleo.onna.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}