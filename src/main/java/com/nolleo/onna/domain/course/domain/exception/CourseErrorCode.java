package com.nolleo.onna.domain.course.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(404, "COURSE_NOT_FOUND", "코스를 찾을 수 없습니다"),
    COURSE_ACCESS_DENIED(403, "COURSE_ACCESS_DENIED", "본인이 생성한 코스만 조회할 수 있습니다"),
    UNKNOWN_START_AREA(400, "UNKNOWN_START_AREA", "지원하지 않는 지역입니다");

    private final int status;
    private final String errorCode;
    private final String message;
}
