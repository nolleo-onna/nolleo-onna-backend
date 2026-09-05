package com.nolleo.onna.common.security.jwt;

import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

// [Security] JWT - access/refresh 생성, 서명+만료 검증, 클레임 파싱 전담.
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final SecretKey key;
    private final long accessTokenExpiryMillis;
    private final long refreshTokenExpiryMillis;

    public JwtProvider(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMillis = props.accessTokenExpirySeconds() * 1000;
        this.refreshTokenExpiryMillis = props.refreshTokenExpirySeconds() * 1000;
    }

    public String createAccessToken(Long userId, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryMillis))
                .signWith(key)
                .compact();
    }

    // jti는 rotation 비교 기준값 → 발급한 jti를 호출측이 Redis에 저장하므로 함께 반환할 수 있도록 createRefreshToken(userId, jti) 형태 사용.
    public String createRefreshToken(Long userId, String jti) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti) // jti 클레임
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiryMillis))
                .signWith(key)
                .compact();
    }

    public String generateJti() {
        return UUID.randomUUID().toString();
    }

    // 서명+만료 검증 후 Claims 반환. 실패 시 JwtException 계열(ExpiredJwtException 등) throw.
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthPrincipal getAuthPrincipal(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_ACCESS);
        Long userId = Long.valueOf(claims.getSubject());
        String roleName = claims.get(CLAIM_ROLE, String.class);
        if(roleName == null) {
            // role 누락 -> NPE(500) 대신 IllegalArgumentException으로 필터에서 401 처리되게 함
            throw new IllegalArgumentException("Missing role claim");
        }
        UserRole role = UserRole.valueOf(roleName);
        return new AuthPrincipal(userId, role);
    }

    public Long getRefreshUserId(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_REFRESH);
        return Long.valueOf(claims.getSubject());
    }

    public String getRefreshJti(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_REFRESH);
        return claims.getId();
    }

    public long getRefreshTokenExpiryMillis() {
        return refreshTokenExpiryMillis;
    }

    private void validateTokenType(Claims claims, String expectedType) {
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedType.equals(tokenType)) {
            throw new IllegalArgumentException("Invalid token type");
        }
    }
}
