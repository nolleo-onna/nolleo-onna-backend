// [Common] 공통 HTTP 에러코드 — 특정 도메인에 귀속되지 않는 전역 오류(400, 404, 405, 500 등) 정의.
// 도메인별 에러코드(e.g. UserErrorCode)와 달리 인프라·Spring MVC 수준의 오류를 담당.
package com.nolleo.onna.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INTERNAL_SERVER_ERROR(500,"SERVER-ERROR","내부 서버 오류"),
    INVALID_INPUT_VALUE(400,"INVALID_REQUEST","@Valid 형식 에러"),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "허용되지 않는 METHOD 입니다"),
    NO_HANDLER_FOUND(404, "NO_HANDLER_FOUND", "존재하지 않는 URL입니다"),
    METHOD_TYPE_MISMATCH(400, "METHOD_TYPE_MISMATCH", "타입이 일치하지 않습니다");

    private final int status;
    private final String errorCode;
    private final String message;

}