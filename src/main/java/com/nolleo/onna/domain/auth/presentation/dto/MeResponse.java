package com.nolleo.onna.domain.auth.presentation.dto;

import com.nolleo.onna.domain.user.domain.model.User;
import com.nolleo.onna.domain.user.domain.model.UserRole;

// [Auth] /me 응답 — access가 HttpOnly라 프론트가 토큰 디코드 불가 → 로그인 직후 이 API로 유저정보 수신.
public record MeResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole());
    }
}
