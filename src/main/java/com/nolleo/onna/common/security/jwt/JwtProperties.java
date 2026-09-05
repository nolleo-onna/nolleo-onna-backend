package com.nolleo.onna.common.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        @NotBlank
        @Size(min = 32, message = "jwt.secret은 HS256 서명을 위해 최소 32자 이상이어야 합니다")
        String secret,

        @Positive
        long accessTokenExpirySeconds,

        @Positive
        long refreshTokenExpirySeconds
) {
    // record 기본 toString은 모든 필드를 노출 → 서명키 로그 유출 방지를 위해 secret만 마스킹.
    @Override
    public String toString() {
        return "JwtProperties[secret=****, accessTokenExpirySeconds=" + accessTokenExpirySeconds
                + ", refreshTokenExpirySeconds=" + refreshTokenExpirySeconds + "]";
    }
}