package com.nolleo.onna.domain.user.domain.model;

// [User] 유저 권한 — Spring Security authority로 변환 시 "ROLE_" 접두사 필요(getAuthority()).
public enum UserRole {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
