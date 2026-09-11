package com.nolleo.onna.domain.event.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements ErrorCode {

    EVENT_NOT_FOUND(404, "EVENT_NOT_FOUND", "행사를 찾을 수 없습니다");

    private final int status;
    private final String errorCode;
    private final String message;
}
