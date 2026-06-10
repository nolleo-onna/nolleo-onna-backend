// [Common] 에러 응답 표준 포맷 — status, errorCode, message + @Valid 검증 실패 시 errorList(필드 단위 오류) 포함.
// fail(ErrorCode), fail(ErrorCode, BindingResult), fail(ErrorCode, String) 세 가지 오버로드로 상황별 사용.
package com.nolleo.onna.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {
    private final int status;
    private final String errorCode;
    private final String message;
    private List<ValidationError> errorList; // @Valid 예외 처리용

    public static ErrorResponseDto of(ErrorCode errorCode) {
        return ErrorResponseDto.builder()
                .status(errorCode.getStatus())
                .errorCode(errorCode.getErrorCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> ResponseEntity<ErrorResponseDto> fail(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.builder()
                        .status(errorCode.getStatus())
                        .errorCode(errorCode.getErrorCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    public static <T> ResponseEntity<ErrorResponseDto> fail(ErrorCode errorCode, BindingResult bindingResult) {
        List<ValidationError> errors = bindingResult.getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.builder()
                        .status(errorCode.getStatus())
                        .errorCode(errorCode.getErrorCode())
                        .errorList(errors)
                        .build());
    }

    public static <T> ResponseEntity<ErrorResponseDto> fail(ErrorCode errorCode, String errorMsg) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.builder()
                        .status(errorCode.getStatus())
                        .errorCode(errorCode.getErrorCode())
                        .message(errorMsg)
                        .build());
    }

    @Getter
    @AllArgsConstructor
    private static class ValidationError {
        private final String field;
        private final String message;
    }
}