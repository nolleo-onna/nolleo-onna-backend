package com.nolleo.onna.domain.auth.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_REFRESH_TOKEN(401, "A001", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(401, "A002", "리프레시 토큰을 찾을 수 없습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_REUSED(401, "A003", "이미 사용된 리프레시 토큰입니다. 보안을 위해 다시 로그인해주세요."),
    OAUTH_PROCESSING_FAILED(401, "A004", "소셜 로그인 처리에 실패했습니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}
