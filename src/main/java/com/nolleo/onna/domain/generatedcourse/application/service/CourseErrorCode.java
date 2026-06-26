package com.nolleo.onna.domain.generatedcourse.application.service;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    UNSUPPORTED_DISTRICT(400, "UNSUPPORTED_DISTRICT", "지원하지 않는 지역입니다."),
    NO_PLACES_IN_DISTRICT(404, "NO_PLACES_IN_DISTRICT", "해당 지역에 등록된 장소가 없습니다."),
    COURSE_NOT_FOUND(404, "COURSE_NOT_FOUND", "코스를 찾을 수 없습니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}
