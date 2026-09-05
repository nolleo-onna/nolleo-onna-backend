package com.nolleo.onna.domain.review.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    MAP_PLACE_NOT_FOUND(404, "R001", "존재하지 않는 장소입니다."),
    DUPLICATE_REVIEW(409, "R002", "이미 해당 장소에 리뷰를 등록하였습니다."),
    REVIEW_NOT_FOUND(404, "R003", "존재하지 않는 리뷰입니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}