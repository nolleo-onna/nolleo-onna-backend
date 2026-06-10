// [Auth] 카카오 유저정보 파싱 — id는 최상위(Long), 이메일/프로필은 kakao_account 중첩.
// 구조: { id, kakao_account: { email, profile: { nickname, profile_image_url } } }
package com.nolleo.onna.domain.auth.infrastructure.oauth;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getExternalId() {
        // id 누락 시 문자열 "null"이 되지 않도록 명시 처리(신분 키 오염 방지).
        Object id = attributes.get("id");
        return id == null ? null : String.valueOf(id);
    }

    @Override
    public String getEmail() {
        Map<String, Object> account = kakaoAccount();
        return account == null ? null : (String) account.get("email");
    }

    @Override
    public String getNickname() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("profile_image_url");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> kakaoAccount() {
        return (Map<String, Object>) attributes.get("kakao_account");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> profile() {
        Map<String, Object> account = kakaoAccount();
        return account == null ? null : (Map<String, Object>) account.get("profile");
    }
}
