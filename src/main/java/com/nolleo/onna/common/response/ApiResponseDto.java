// [Common] 성공 응답 표준 포맷 — status, message, data 3필드 구조. null 필드는 JSON에서 자동 제외(NON_NULL).
// 컨트롤러에서 ApiResponseDto.success(200, "조회 성공", data) 형태로 사용.
package com.nolleo.onna.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDto<T> {

    private int status;       // 상태 코드 (예: 200, 400)
    private String message;   // 반환 메시지
    private T data;           // 반환 데이터

    // 성공 시
    public static <T> ResponseEntity<ApiResponseDto<T>> success(int status, String message, T data) {
        return ResponseEntity
                .status(status)
                .body(new ApiResponseDto<>(status, message, data));
    }

}