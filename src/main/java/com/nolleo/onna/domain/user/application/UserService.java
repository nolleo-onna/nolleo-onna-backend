package com.nolleo.onna.domain.user.application;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import com.nolleo.onna.domain.user.domain.exception.UserErrorCode;
import com.nolleo.onna.domain.user.domain.model.OAuthProvider;
import com.nolleo.onna.domain.user.domain.model.User;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// [User] 유저 유즈케이스 — 소셜 로그인 시 upsert, 단건 조회 담당.
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserJpaRepository userJpaRepository;

    @Transactional
    public User oauthLogin(String externalId, OAuthProvider provider,
                           String email, String nickname, String profileImageUrl) {
        UserEntity entity = userJpaRepository.findByExternalIdAndProvider(externalId, provider)
                .map(existing -> {
                    validateActiveUser(existing);
                    existing.updateProfile(nickname, profileImageUrl); // 더티 체킹으로 갱신
                    return existing;
                })
                .orElseGet(() -> createOrReload(externalId, provider, email, nickname, profileImageUrl));
        return entity.toDomain();
    }

    private UserEntity createOrReload(String externalId, OAuthProvider provider,
                                      String email, String nickname, String profileImageUrl) {
        userJpaRepository.insertIgnoreForOAuthLogin(
                externalId,
                provider.name().toLowerCase(),
                email,
                nickname,
                profileImageUrl
        );
        UserEntity entity = userJpaRepository.findByExternalIdAndProvider(externalId, provider)
                .orElseThrow( () -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        validateActiveUser(entity);
        entity.updateProfile(nickname, profileImageUrl);
        return entity;
    }

    // 회원 탈퇴(soft delete) - 본인 계정, 이미 탈퇴한 상태면 ALREADY_DELETED_USER.
    @Transactional
    public void withdraw(Long userId) {
        UserEntity entity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        validateActiveUser(entity);
        entity.softDelete("self"); // 더티 체킹으로 반영
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userJpaRepository.findById(userId)
                .map(entity -> {
                    validateActiveUser(entity);
                    return entity.toDomain();
                })
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateActiveUser(UserEntity entity) {
        if (entity.isDeleted()) {
            throw new BusinessException(UserErrorCode.ALREADY_DELETED_USER);
        }
    }

}
