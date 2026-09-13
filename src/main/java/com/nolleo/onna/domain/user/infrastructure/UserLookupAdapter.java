package com.nolleo.onna.domain.user.infrastructure;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookupPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<UserProfile> findById(Long userId) {
        return userJpaRepository.findById(userId)
                .filter(entity -> !entity.isDeleted())
                .map(UserLookupAdapter::toProfile);
    }

    @Override
    public Map<Long, UserProfile> findByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userJpaRepository.findAllById(userIds).stream()
                .filter(entity -> !entity.isDeleted())
                .collect(Collectors.toMap(UserEntity::getId, UserLookupAdapter::toProfile));
    }

    private static UserProfile toProfile(UserEntity entity) {
        return new UserProfile(entity.getNickname(), entity.getProfileImageUrl());
    }
}
