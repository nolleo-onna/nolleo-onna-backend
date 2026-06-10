package com.nolleo.onna.common.security;

import com.nolleo.onna.domain.user.domain.model.UserRole;

// [Security] 인증된 주체 — access JWT 클레임에서 복원. DB 조회 없이 userId/role만 보유(stateless).
// 컨트롤러에서 @AuthenticationPrincipal AuthPrincipal 로 주입받아 사용.
public record AuthPrincipal(Long userId, UserRole role) {
}
