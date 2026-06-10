package com.nolleo.onna.domain.user.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

// [User] 순수 도메인 모델 — JPA/프레임워크 의존 없음. UserEntity와 1:1 대응.
// restore(...) : DB 조회 후 엔티티 → 도메인 복원용 (id 포함).
// updateProfile(...) : 재로그인 시 provider가 내려준 최신 프로필 반영(불변 객체라 새 인스턴스 반환).
@Getter
@ToString
@Builder
public class User {

    private final Long id;
    private final String externalId;
    private final OAuthProvider provider;
    private final String email;
    private final String nickname;
    private final String profileImageUrl;
    private final UserRole role;

    public static User restore(Long id, String externalId, OAuthProvider provider,
                               String email, String nickname, String profileImageUrl, UserRole role) {
        return User.builder()
                .id(id)
                .externalId(externalId)
                .provider(provider)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(role)
                .build();
    }

    public User updateProfile(String nickname, String profileImageUrl) {
        return User.builder()
                .id(this.id)
                .externalId(this.externalId)
                .provider(this.provider)
                .email(this.email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(this.role)
                .build();
    }
}
