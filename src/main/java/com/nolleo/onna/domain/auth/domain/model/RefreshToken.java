package com.nolleo.onna.domain.auth.domain.model;

// [Auth] refresh 토큰 도메인 모델 — Redis 저장 단위(키: userId, 값: 현재 유효한 jti).
// rotation: 재발급마다 jti 교체 → 직전 refresh는 무효.
// 재사용 탐지: 들어온 refresh의 jti가 저장된 jti와 다르면 탈취 의심 → 호출측이 전체 폐기.
//   (실제 jti 비교·교체는 원자성 보장을 위해 RedisTokenAdapter의 rotateIfMatches 스크립트에서 수행)
public record RefreshToken(Long userId, String jti) {

    public static RefreshToken issue(Long userId, String jti) {
        return new RefreshToken(userId, jti);
    }
}
