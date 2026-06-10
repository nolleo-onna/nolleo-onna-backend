package com.nolleo.onna.domain.auth.application;


import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.auth.domain.exception.AuthErrorCode;
import com.nolleo.onna.domain.auth.domain.model.AuthTokens;
import com.nolleo.onna.domain.auth.domain.model.RefreshToken;
import com.nolleo.onna.domain.auth.domain.repository.RefreshTokenRepository;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// [Auth] 토큰 발급·검증·rotation 핵심 — JwtProvider(생성/파싱) + RefreshTokenRepository(저장) 조합.
// issue : access+refresh 발급, refresh의 jti를 Redis 저장(기존 값 덮어씀 = rotation).
// validateRefreshAndGetUserId : refresh 서명/만료 검증 → Redis 저장 jti와 비교.
//   - Redis에 없음(로그아웃/만료) → REFRESH_TOKEN_NOT_FOUND
//   - jti 불일치(폐기된 토큰 재사용=탈취 의심) → 해당 유저 전체 폐기 후 REFRESH_TOKEN_REUSED
// delete : 로그아웃 등 강제 폐기.
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthTokens issue(Long userId, UserRole role) {
        String jti = jwtProvider.generateJti();
        String accessToken = jwtProvider.createAccessToken(userId, role);
        String refreshToken = jwtProvider.createRefreshToken(userId, jti);

        refreshTokenRepository.save(RefreshToken.issue(userId, jti),
                jwtProvider.getRefreshTokenExpiryMillis());

        return AuthTokens.of(accessToken, refreshToken);
    }

    public Long getRefreshUserId(String refreshTokenValue) {
        try {
            return jwtProvider.getRefreshUserId(refreshTokenValue);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    // TODO: strict refresh rotation이라 정상 중복 refresh 요청(재시도/멀티탭)도 재사용으로 간주돼 강제 로그아웃될 수 있음.
    //       추후 grace window(직전 jti 단기 허용) 또는 멱등 재발급 고려.

    public AuthTokens reissue(String refreshTokenValue, UserRole role) {
        Long userId;
        String incomingJti;
        try {
            userId = jwtProvider.getRefreshUserId(refreshTokenValue);
            incomingJti = jwtProvider.getRefreshJti(refreshTokenValue);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newJti = jwtProvider.generateJti();
        RefreshTokenRepository.RotationResult result = refreshTokenRepository.rotateIfMatches(
                userId,
                incomingJti,
                newJti,
                jwtProvider.getRefreshTokenExpiryMillis());

        if (result == RefreshTokenRepository.RotationResult.NOT_FOUND) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (result == RefreshTokenRepository.RotationResult.MISMATCHED) {
            // 폐기된 refresh 재사용 → 탈취 의심, 해당 유저 토큰 전체 폐기
            refreshTokenRepository.deleteByUserId(userId);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }

        String accessToken = jwtProvider.createAccessToken(userId, role);
        String refreshToken = jwtProvider.createRefreshToken(userId, newJti);
        return AuthTokens.of(accessToken, refreshToken);
    }

    public void delete(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

}
