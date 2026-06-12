// [Auth] OAuth 로그인 principal — CustomOAuth2UserService가 upsert 후 반환, SuccessHandler가 userId/role을 꺼내 JWT 발급.
// OAuth2User를 구현해 Spring Security 컨텍스트에 그대로 올라간다. getName()은 userId(문자열).
package com.nolleo.onna.domain.auth.infrastructure.oauth;

import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

public class OAuth2UserPrincipal implements OAuth2User {

    private final Long userId;
    private final UserRole role;
    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(Long userId, UserRole role, Map<String, Object> attributes) {
        this.userId = userId;
        this.role = role;
        // 외부 변조 방지 + null-safe. provider attributes는 null 값을 포함할 수 있어 Map.copyOf는 부적합.
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
