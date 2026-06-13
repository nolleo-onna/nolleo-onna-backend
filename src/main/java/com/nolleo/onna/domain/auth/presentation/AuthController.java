package com.nolleo.onna.domain.auth.presentation;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.CookieFactory;
import com.nolleo.onna.common.security.jwt.JwtProperties;
import com.nolleo.onna.domain.auth.application.AuthService;
import com.nolleo.onna.domain.auth.domain.exception.AuthErrorCode;
import com.nolleo.onna.domain.auth.domain.model.AuthTokens;
import com.nolleo.onna.domain.auth.presentation.dto.GetUserResponse;
import com.nolleo.onna.domain.user.application.UserService;
import com.nolleo.onna.domain.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// [Auth] 인증 API — 재발급/로그아웃/내정보. (OAuth 로그인 시작·콜백은 Spring Security가 처리하므로 컨트롤러 없음)
// WebConfig가 "/api/v1" 프리픽스를 자동 부여 → 실제 경로는 /api/v1/auth/**.
// refresh/logout 시 Set-Cookie로 토큰 쿠키를 갱신/삭제.
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CookieFactory cookieFactory;
    private final JwtProperties jwtProperties;

    // 재발급: refresh 쿠키 → rotation 발급 → 새 access/refresh 쿠키 내려줌.
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<Void>> refresh(
            @CookieValue(name = CookieFactory.REFRESH_TOKEN, required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        AuthTokens tokens = authService.reissue(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.accessCookie(tokens.accessToken(), jwtProperties.accessTokenExpirySeconds()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.refreshCookie(tokens.refreshToken(), jwtProperties.refreshTokenExpirySeconds()).toString())
                .body(ApiResponseDto.<Void>success(200, "토큰 재발급 성공", null).getBody());
    }

    // 로그아웃: Redis refresh 삭제 + access/refresh 쿠키 만료.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.logout(principal.userId());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expireAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expireRefreshCookie().toString())
                .body(ApiResponseDto.<Void>success(200, "로그아웃 성공", null).getBody());
    }

    // 내 정보: HttpOnly라 프론트가 디코드 못 하므로 서버에서 조회해 전달.
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<GetUserResponse>> me(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = userService.getById(principal.userId());
        return ApiResponseDto.success(200, "유저 정보 조회 성공", GetUserResponse.from(user));
    }
}
