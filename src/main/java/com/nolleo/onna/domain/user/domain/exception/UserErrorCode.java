package com.nolleo.onna.domain.user.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;

// [User] User 도메인 전용 에러코드 — ErrorCode 인터페이스 구현체.
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다."),
    ALREADY_DELETED_USER(409, "U002", "이미 탈퇴한 사용자입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(400, "U003", "지원하지 않는 OAuth 제공자입니다."),
    UNSUPPORTED_USER_ROLE(400, "U008", "지원하지 않는 사용자 권한입니다."),
    OAUTH_AUTHENTICATION_FAILED(401, "U004", "OAuth 인증에 실패했습니다."),
    INVALID_TOKEN(401, "U005", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "U006", "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(401, "U007", "토큰을 찾을 수 없습니다.");

    private final int status;
    private final String errorCode;
    private final String message;

    UserErrorCode(int status, String errorCode, String message) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }

    @Override public int getStatus()       { return status; }
    @Override public String getErrorCode() { return errorCode; }
    @Override public String getMessage()   { return message; }
}
