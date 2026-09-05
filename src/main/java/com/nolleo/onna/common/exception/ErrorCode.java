// [Common] 에러코드 표준 인터페이스 — 모든 도메인별 ErrorCode enum이 구현해야 할 규격.
// HTTP 상태코드(status), 식별자(errorCode), 메시지(message) 3가지를 강제해 GlobalExceptionHandler에서 일관 처리.
package com.nolleo.onna.common.exception;

public interface ErrorCode {
    int getStatus();
    String getErrorCode();
    String getMessage();
}