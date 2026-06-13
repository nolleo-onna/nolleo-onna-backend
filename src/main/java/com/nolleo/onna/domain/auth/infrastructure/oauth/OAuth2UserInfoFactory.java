package com.nolleo.onna.domain.auth.infrastructure.oauth;

import com.nolleo.onna.domain.user.domain.model.OAuthProvider;

import java.util.Map;

// [Auth] registrationId("google"/"kakao"/"naver") → 알맞은 OAuth2UserInfo 구현체 생성.
// 미지원 provider면 UNSUPPORTED_OAUTH_PROVIDER 예외.
public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo of(OAuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case KAKAO -> new KakaoOAuth2UserInfo(attributes);
            case NAVER -> new NaverOAuth2UserInfo(attributes);
        };
    }
}
