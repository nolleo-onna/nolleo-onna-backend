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

    // AI 코스 생성 — 외부 의존성·동시성
    AI_SERVICE_UNAVAILABLE(503, "AI_SERVICE_UNAVAILABLE", "AI 서비스에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해주세요"),
    GENERATION_LIMIT_UNAVAILABLE(503, "GENERATION_LIMIT_UNAVAILABLE", "생성 횟수를 확인할 수 없어 코스를 만들 수 없습니다. 잠시 후 다시 시도해주세요"),
    CHAT_LIMIT_UNAVAILABLE(503, "CHAT_LIMIT_UNAVAILABLE", "이용 횟수를 확인할 수 없어 대화를 진행할 수 없습니다. 잠시 후 다시 시도해주세요"),
    COURSE_GENERATION_IN_PROGRESS(409, "COURSE_GENERATION_IN_PROGRESS", "이 대화의 코스를 이미 만들고 있어요. 잠시 후 내 코스 목록을 확인해주세요"),
    NO_SPOT_CANDIDATES(404, "NO_SPOT_CANDIDATES", "조건에 맞는 방문 스팟을 찾지 못했습니다. 지역이나 조건을 바꿔 다시 시도해주세요"),

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
