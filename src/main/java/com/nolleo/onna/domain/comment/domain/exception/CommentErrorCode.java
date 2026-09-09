package com.nolleo.onna.domain.comment.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    COMMENT_NOT_FOUND(404, "CM001", "존재하지 않는 댓글입니다."),
    COMMENT_ACCESS_DENIED(403, "CM002", "댓글에 대한 권한이 없습니다."),
    INVALID_PARENT_COMMENT(400, "CM003", "대댓글에는 답글을 달 수 없습니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}
