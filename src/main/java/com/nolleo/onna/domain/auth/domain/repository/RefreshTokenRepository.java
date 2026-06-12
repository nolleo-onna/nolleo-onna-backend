package com.nolleo.onna.domain.auth.domain.repository;

import com.nolleo.onna.domain.auth.domain.model.RefreshToken;

import java.util.Optional;

// [Auth] refresh 저장소 포트 — 구현은 infrastructure(Redis). 도메인은 저장 기술을 모름.
public interface RefreshTokenRepository {

    // 발급/rotation시 userId -> jti 저장(TTL=refresh 만료시간)
    void save(RefreshToken refreshToken, long ttlMillis);

    // 재발급 검증 시 현재 유효한 jti 조회
    Optional<RefreshToken> findByUserId(Long userId);

    RotationResult rotateIfMatches(Long userId, String externalJti, String newJti, long ttlMillis);

    // delete : 로그아웃 / 재사용 탐지로 인한 강제 폐기 시 삭제.
    void deleteByUserId(Long userId);

    enum RotationResult {
        ROTATED,
        NOT_FOUND,
        MISMATCHED
    }

}
