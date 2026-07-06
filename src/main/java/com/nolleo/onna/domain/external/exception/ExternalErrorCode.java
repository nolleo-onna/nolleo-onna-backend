package com.nolleo.onna.domain.external.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalErrorCode implements ErrorCode {

    DISTRICT_NOT_FOUND(400, "EX001", "지원하지 않는 지역입니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}
