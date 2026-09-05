package com.nolleo.onna.domain.user.infrastructure;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookupPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<UserProfile> findById(Long userId) {
        return userJpaRepository.findById(userId)
                .filter(entity -> !entity.isDeleted())
                .map(entity -> new UserProfile(entity.getNickname(), entity.getProfileImageUrl()));
    }
}
