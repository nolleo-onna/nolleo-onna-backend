package com.nolleo.onna.domain.favorite.domain.exception;

import com.nolleo.onna.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FavoriteErrorCode implements ErrorCode {

    MAP_PLACE_NOT_FOUND(404, "F001", "존재하지 않는 장소입니다.");

    private final int status;
    private final String errorCode;
    private final String message;
}
