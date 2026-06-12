package com.nolleo.onna.common.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// [Security] HttpOnly 쿠키 생성/삭제 유틸 — access/refresh 토큰을 쿠키로 내려보냄.
// access  : path "/" (모든 요청에 동봉)
// refresh : path "/api/v1/auth/refresh" (재발급 때만 전송 → 노출 면적 축소)
// Secure/SameSite/Domain은 CookieProperties(배포 환경별)에서 주입. expire()는 Max-Age 0 삭제용.
@Component
public class CookieFactory {
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    private static final String ACCESS_PATH = "/";
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";

    private final CookieProperties props;

    public CookieFactory(CookieProperties props) {
        this.props = props;
    }

    public ResponseCookie accessCookie(String token, long maxAgeSeconds) {
        return build(ACCESS_TOKEN, token, ACCESS_PATH, maxAgeSeconds);
    }

    public ResponseCookie refreshCookie(String token, long maxAgeSeconds) {
        return build(REFRESH_TOKEN, token, REFRESH_PATH, maxAgeSeconds);
    }

    public ResponseCookie expireAccessCookie() {
        return build(ACCESS_TOKEN, "", ACCESS_PATH, 0);
    }

    public ResponseCookie expireRefreshCookie() {
        return build(REFRESH_TOKEN, "", REFRESH_PATH, 0);
    }

    private ResponseCookie build(String name, String value, String path, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.secure())
                .sameSite(props.sameSite())
                .path(path)
                .maxAge(maxAgeSeconds);
        if (props.domain() != null && !props.domain().isBlank()) {
            builder.domain(props.domain());
        }
        return builder.build();
    }
}
