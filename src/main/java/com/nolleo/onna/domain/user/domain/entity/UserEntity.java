package com.nolleo.onna.domain.user.domain.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.SoftDeleteAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.user.domain.converter.OAuthProviderConverter;
import com.nolleo.onna.domain.user.domain.converter.UserRoleConverter;
import com.nolleo.onna.domain.user.domain.model.OAuthProvider;
import com.nolleo.onna.domain.user.domain.model.User;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

// [User] JPA 엔티티 (mb_user_info 테이블) — DB 영속성 전담. 도메인 모델(User)과 1:1 대응하나 JPA 관심사만 보유.
// toDomain() : 엔티티 → 도메인 복원 (조회 후 비즈니스 로직 처리를 위해 순수 User 객체로 변환).
@Entity
@Table(name = "mb_user_info",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_provider_external",
                columnNames = {"provider", "external_id"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;

    @Convert(converter = OAuthProviderConverter.class)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "nickname", length = 80)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Convert(converter = UserRoleConverter.class)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    // 마지막 활동 시각 — DDL의 last_active_at(인덱스 대상). 갱신 로직은 추후(활동 추적 기능) 추가.
    @Column(name = "last_active_at")
    private OffsetDateTime lastActiveAt;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    @Embedded
    private SoftDeleteAudit softDeleteAudit;

    public User toDomain() {
        return User.restore(id, externalId, provider, email, nickname, profileImageUrl, role);
    }

    public boolean isDeleted() {
        return softDeleteAudit != null && softDeleteAudit.isDeleted();
    }

    // 회원 탈퇴(soft delete) — deleted_at/by 기록 + updated_at 갱신. 이미 탈퇴면 SoftDeleteAudit가 무시.
    public void softDelete(String deletedBy) {
        if (this.softDeleteAudit == null) { // @Embedded all-null 행은 null로 로드될 수 있어 방어.
            this.softDeleteAudit = SoftDeleteAudit.active();
        }
        this.softDeleteAudit.softDelete(deletedBy);
        this.updateAudit.touch(deletedBy);
    }

    // 재로그인 시 provider 최신 프로필 반영 — createAudit는 보존, updateAudit만 갱신(touch).
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.updateAudit.touch("system");
    }
}
