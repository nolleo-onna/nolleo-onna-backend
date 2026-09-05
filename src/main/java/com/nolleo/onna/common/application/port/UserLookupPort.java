package com.nolleo.onna.common.application.port;

import java.util.Optional;

public interface UserLookupPort {
    record UserProfile(String nickname, String profileImageUrl) {}
    Optional<UserProfile> findById(Long userId);
}
