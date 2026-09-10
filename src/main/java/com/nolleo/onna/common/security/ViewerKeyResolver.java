package com.nolleo.onna.common.security;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 조회수 중복 제한에 쓰는 조회자 식별 키.
 *
 * 로그인: u:{userId} / 비로그인: ip:{SHA-256(클라이언트 IP) 앞 16자}.
 * 원본 IP를 Redis 키에 그대로 남기지 않기 위한 해시이며, TTL 10분짜리 키에만 쓰인다 — 암호학적 익명화는 아니다.
 * 운영(docker 프로필)은 server.forward-headers-strategy=native라 Caddy 뒤에서도 getRemoteAddr가 실제 클라이언트 IP다.
 */
public final class ViewerKeyResolver {

    private static final int IP_HASH_LENGTH = 16;

    private ViewerKeyResolver() {
    }

    public static String resolve(AuthPrincipal principal, HttpServletRequest request) {
        if (principal != null && principal.userId() != null) {
            return "u:" + principal.userId();
        }
        return "ip:" + sha256Hex(request.getRemoteAddr()).substring(0, IP_HASH_LENGTH);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
