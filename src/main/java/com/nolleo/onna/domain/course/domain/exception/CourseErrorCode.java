package com.nolleo.onna.domain.course.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(404, "COURSE_NOT_FOUND", "코스를 찾을 수 없습니다"),
    COURSE_ACCESS_DENIED(403, "COURSE_ACCESS_DENIED", "본인이 생성한 코스만 조회·수정할 수 있습니다"),
    UNKNOWN_START_AREA(400, "UNKNOWN_START_AREA", "지원하지 않는 지역입니다"),

    // 코스 수정 — 제목·소개
    COURSE_TITLE_INVALID(400, "COURSE_TITLE_INVALID", "코스 제목은 1~" + Course.MAX_TITLE_LENGTH + "자로 입력해야 합니다"),
    COURSE_DESCRIPTION_TOO_LONG(400, "COURSE_DESCRIPTION_TOO_LONG", "코스 소개는 최대 " + Course.MAX_DESCRIPTION_LENGTH + "자까지 입력할 수 있습니다"),

    // 코스 수정 — 방문 스팟 목록
    COURSE_ITEM_EMPTY(400, "COURSE_ITEM_EMPTY", "코스에는 최소 1개의 방문 스팟이 필요합니다"),
    COURSE_ITEM_LIMIT_EXCEEDED(400, "COURSE_ITEM_LIMIT_EXCEEDED", "코스의 방문 스팟은 최대 " + CoursePlaces.MAX_ITEMS + "개까지 담을 수 있습니다"),
    COURSE_ITEM_DUPLICATED(400, "COURSE_ITEM_DUPLICATED", "같은 장소를 두 번 담을 수 없습니다"),
    COURSE_PLACE_NOT_FOUND(400, "COURSE_PLACE_NOT_FOUND", "코스에 담을 수 없는 장소입니다"),
    COURSE_PLACE_TYPE_NOT_SUPPORTED(400, "COURSE_PLACE_TYPE_NOT_SUPPORTED", "아직 지원하지 않는 장소 유형입니다");

    private final int status;
    private final String errorCode;
    private final String message;
}
