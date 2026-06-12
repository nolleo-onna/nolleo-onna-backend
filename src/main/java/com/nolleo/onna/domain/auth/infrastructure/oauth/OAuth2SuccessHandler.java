// [Auth] OAuth 로그인 성공 핸들러 — 자체 JWT(access/refresh) 발급 → HttpOnly 쿠키 set → 프론트 콜백으로 리다이렉트.
// 토큰 값은 쿠키로만 전달(URL 노출 X). 프론트는 리다이렉트 후 /api/v1/auth/me 로 유저정보 조회.
package com.nolleo.onna.domain.auth.infrastructure.oauth;

import com.nolleo.onna.common.security.CookieFactory;
import com.nolleo.onna.common.security.jwt.JwtProperties;
import com.nolleo.onna.domain.auth.application.TokenService;
import com.nolleo.onna.domain.auth.domain.model.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final CookieFactory cookieFactory;
    private final JwtProperties jwtProperties;

    @Value("${app.oauth2.success-redirect-uri}")
    private String successRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

        AuthTokens tokens = tokenService.issue(principal.getUserId(), principal.getRole());

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieFactory.accessCookie(tokens.accessToken(), jwtProperties.accessTokenExpirySeconds()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieFactory.refreshCookie(tokens.refreshToken(), jwtProperties.refreshTokenExpirySeconds()).toString());

        getRedirectStrategy().sendRedirect(request, response, successRedirectUri);
    }
}
