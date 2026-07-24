package com.nolleo.onna.domain.course.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(404, "COURSE_NOT_FOUND", "코스를 찾을 수 없습니다");

    private final int status;
    private final String errorCode;
    private final String message;
}
