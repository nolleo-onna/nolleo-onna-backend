package com.nolleo.onna.domain.user.domain.repository;

import com.nolleo.onna.domain.user.domain.model.OAuthProvider;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// [User] Spring Data JPA 인터페이스 — UserEntity 기준 쿼리 자동 생성.
// findByExternalIdAndProvider : 소셜 로그인 시 기존 회원 여부 확인에 사용 (external_id + provider 복합 조건).
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByExternalIdAndProvider(String externalId, OAuthProvider provider);

    @Modifying
    @Query(value = """
            INSERT INTO mb_user_info (
                external_id,
                provider,
                email,
                nickname,
                profile_image_url,
                role,
                created_at,
                created_by,
                updated_at,
                updated_by
            )
            VALUES (
                :externalId,
                :provider,
                :email,
                :nickname,
                :profileImageUrl,
                'user',
                CURRENT_TIMESTAMP,
                'system',
                CURRENT_TIMESTAMP,
                'system'
            )
            ON CONFLICT (provider, external_id) DO NOTHING
            """, nativeQuery = true)
    void insertIgnoreForOAuthLogin(@Param("externalId") String externalId,
                                   @Param("provider") String provider,
                                   @Param("email") String email,
                                   @Param("nickname") String nickname,
                                   @Param("profileImageUrl") String profileImageUrl);
}
