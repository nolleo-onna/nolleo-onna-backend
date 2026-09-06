package com.nolleo.onna.domain.post.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

    POST_NOT_FOUND(404, "PT001", "존재하지 않는 게시글입니다."),
    POST_ACCESS_DENIED(403, "PT002", "게시글에 대한 권한이 없습니다."),
    ALREADY_LIKED(409, "PT003", "이미 좋아요한 게시글입니다."),
    ALREADY_REPORTED(409, "PT004", "이미 신고한 게시글입니다."),
    CANNOT_REPORT_OWN_POST(400, "PT005", "본인의 게시글은 신고할 수 없습니다."),
    TOO_MANY_IMAGES(400, "PT006", "이미지는 최대 5장까지만 첨부할 수 있습니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}
