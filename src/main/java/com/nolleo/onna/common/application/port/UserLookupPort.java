package com.nolleo.onna.common.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface UserLookupPort {
    record UserProfile(String nickname, String profileImageUrl) {}
    Optional<UserProfile> findById(Long userId);

    /**
     * 회원 id 목록으로 프로필을 일괄 조회한다 — 목록 응답에서 작성자를 붙일 때 N+1을 피한다.
     * 탈퇴(소프트 삭제)했거나 없는 id는 결과 맵에서 빠진다.
     */
    Map<Long, UserProfile> findByIds(Collection<Long> userIds);
}
