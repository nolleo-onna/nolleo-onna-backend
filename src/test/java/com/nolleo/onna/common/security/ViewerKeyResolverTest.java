package com.nolleo.onna.common.security;

import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ViewerKeyResolverTest {

    private static MockHttpServletRequest requestFrom(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    @DisplayName("로그인 사용자는 회원 id로 식별한다 — 접속 IP가 달라도 같은 키")
    void resolve_usesUserId_whenLoggedIn() {
        AuthPrincipal principal = new AuthPrincipal(7L, UserRole.USER);

        assertThat(ViewerKeyResolver.resolve(principal, requestFrom("203.0.113.9"))).isEqualTo("u:7");
        assertThat(ViewerKeyResolver.resolve(principal, requestFrom("198.51.100.1"))).isEqualTo("u:7");
    }

    @Test
    @DisplayName("비로그인은 IP 해시로 식별한다 — 같은 IP는 같은 키, 다른 IP는 다른 키, 원본 IP는 키에 남지 않는다")
    void resolve_usesIpHash_whenAnonymous() {
        String key = ViewerKeyResolver.resolve(null, requestFrom("203.0.113.9"));

        assertThat(key).startsWith("ip:").hasSize(3 + 16).doesNotContain("203.0.113.9");
        assertThat(ViewerKeyResolver.resolve(null, requestFrom("203.0.113.9"))).isEqualTo(key);
        assertThat(ViewerKeyResolver.resolve(null, requestFrom("198.51.100.1"))).isNotEqualTo(key);
    }
}
