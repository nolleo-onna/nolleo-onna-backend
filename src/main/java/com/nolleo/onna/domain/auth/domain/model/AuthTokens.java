package com.nolleo.onna.domain.auth.domain.model;

// [Auth] 발급된 토큰 쌍 VO — TokenService가 발급 결과를 묶어 반환, 컨트롤러/핸들러가 쿠키로 내려줌.
public record AuthTokens(String accessToken, String refreshToken) {

    public static AuthTokens of(String accessToken, String refreshToken) {
        return new AuthTokens(accessToken, refreshToken);
    }
}
