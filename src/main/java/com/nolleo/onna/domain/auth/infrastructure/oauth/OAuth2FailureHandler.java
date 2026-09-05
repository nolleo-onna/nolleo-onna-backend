// [Auth] OAuth 로그인 실패 핸들러 — 프론트 에러 페이지로 리다이렉트(민감정보 노출 없이 간단한 플래그만).
package com.nolleo.onna.domain.auth.infrastructure.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j(topic = "OAuth2")
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-uri}")
    private String failureRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth 로그인 실패 - {}", exception.getMessage());
        getRedirectStrategy().sendRedirect(request, response, failureRedirectUri);
    }
}
