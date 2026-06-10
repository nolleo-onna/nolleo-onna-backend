package com.nolleo.onna.domain.user.domain.model;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.user.domain.exception.UserErrorCode;

// [User] 소셜 로그인 제공자 — 소셜 로그인만 지원(이메일 가입 없음).
// from(registrationId) : Spring Security registrationId("google"/"kakao"/"naver") → enum 변환.
//                        매핑 실패 시 UNSUPPORTED_OAUTH_PROVIDER 예외.
public enum OAuthProvider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static OAuthProvider from(String registrationId) {
        try {
            return OAuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }
}
