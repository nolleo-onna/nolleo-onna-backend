// [Common] 전역 예외 핸들러 — @RestControllerAdvice로 앱 전체 예외를 가로채 ErrorResponseDto로 변환해 응답.
// BusinessException(도메인 오류), @Valid 검증 오류, HTTP 메서드 오류, 500 내부 오류를 계층별로 처리.
package com.nolleo.onna.common.exception;

import com.nolleo.onna.common.response.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Slf4j(topic = "Exception")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponseDto> handleCustomException(BusinessException e) {
        log.warn("SERVICE_ERROR - Code: {}, Message: {}", e.getErrorCode().getErrorCode(),e.getErrorCode().getMessage());
        return ErrorResponseDto.fail(e.getErrorCode());
    }

    // @Valid 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidExceptionException(MethodArgumentNotValidException e) {
        log.warn("VALID_ERROR");
        return ErrorResponseDto.fail(CommonErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
    }

    //BAD_REQUEST
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("METHOD_NOT_ALLOWED");
        return ErrorResponseDto.fail(CommonErrorCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    // NoHandlerFoundException
    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<ErrorResponseDto> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("존재하지 않는 URL 입니다");
        return ErrorResponseDto.fail(CommonErrorCode.NO_HANDLER_FOUND, e.getMessage());
    }
    // MethodArgumentTypeMismatchException
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("타입이 일치하지 않습니다");
        return  ErrorResponseDto.fail(CommonErrorCode.METHOD_TYPE_MISMATCH, e.getMessage());
    }

    // IllegalArgumentException — 도메인 객체 내부 방어 검증 실패 (프로그래밍 오류)
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 인자 오류 (프로그래밍 버그 의심) - {}", e.getMessage(), e);
        return ErrorResponseDto.fail(CommonErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    // INTERNAL_SERVER_ERROR (500 에러)
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        log.error("내부 서버 오류 - {}", e.getMessage(), e);
        return ErrorResponseDto.fail(CommonErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }
}