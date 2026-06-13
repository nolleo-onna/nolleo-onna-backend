package com.nolleo.onna.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// [Security] 쿠키 속성 바인딩 — app.cookie.* 매핑.
// 배포 환경 미정이므로 secure/sameSite/domain은 프로파일별 yml로 주입(로컬은 secure=false).
// domain이 빈 문자열이면 Domain 속성 미지정(현재 호스트 기준).
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        boolean secure,
        String sameSite,
        String domain
) {
}
