package com.nolleo.onna.domain.image.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    INVALID_FILE_TYPE(400, "IMG001", "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, webp만 허용)"),
    FILE_SIZE_EXCEEDED(400, "IMG002", "파일 크기는 10MB를 초과할 수 없습니다."),
    UPLOAD_FAILED(500, "IMG003", "이미지 업로드에 실패했습니다."),
    TOO_MANY_IMAGES(400, "IMG004", "이미지는 최대 5장까지만 업로드할 수 있습니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}
