package com.nolleo.onna.domain.auth.application;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.auth.domain.model.AuthTokens;
import com.nolleo.onna.domain.user.application.UserService;
import com.nolleo.onna.domain.user.domain.exception.UserErrorCode;
import com.nolleo.onna.domain.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// [Auth] 인증 유즈케이스 오케스트레이션 — 컨트롤러 진입점.
// reissue : refresh 검증(rotation/재사용탐지) → 유저 role 조회 → 새 access+refresh 발급(새 jti로 rotation).
// logout  : Redis refresh 삭제(즉시 무효화). access는 짧은 만료라 자연 소멸 + 쿠키 삭제는 컨트롤러가 처리.
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenService tokenService;
    private final UserService userService;

    public AuthTokens reissue(String refreshTokenValue) {
        Long userId = tokenService.getRefreshUserId(refreshTokenValue);
        User user;
        try {
            user = userService.getById(userId);
        } catch (BusinessException e) {
            if (e.getErrorCode() == UserErrorCode.ALREADY_DELETED_USER) {
                tokenService.delete(userId);
            }
            throw e;
        }
        return tokenService.reissue(refreshTokenValue, user.getRole());
    }

    public void logout(Long userId) {
        tokenService.delete(userId);
    }
}
