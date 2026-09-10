package com.nolleo.onna.domain.course.domain.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 코스 공유 링크 토큰 발급 — URL-safe 랜덤 32자.
 *
 * SecureRandom 24바이트를 base64url(패딩 없음)로 인코딩하면 정확히 32자가 된다.
 * generated_courses.share_token VARCHAR(64) 안에 들어가고, UUID(36자)보다 짧으면서 추측 불가능하다.
 * 도메인 서비스이지만 상태가 없으므로 정적 메서드로 둔다.
 */
public final class ShareTokenGenerator {

    public static final int TOKEN_LENGTH = 32;

    private static final int RANDOM_BYTES = 24;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private ShareTokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
