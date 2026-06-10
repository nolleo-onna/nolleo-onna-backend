// [Auth] 네이버 유저정보 파싱 — 표준 OIDC 아님. 실제 정보가 response 안에 중첩됨.
// 구조: { resultcode, message, response: { id, email, nickname, profile_image } }
// (user-name-attribute=response 로 yml 설정 → attributes 자체가 response 맵으로 들어옴)
package com.nolleo.onna.domain.auth.infrastructure.oauth;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        // user-name-attribute=response 설정 시 Spring이 response 맵을 attributes로 넘겨줌.
        // 안전하게 response 키가 있으면 펼치고, 없으면 attributes 그대로 사용.
        Object inner = attributes.get("response");
        this.response = inner instanceof Map ? (Map<String, Object>) inner : attributes;
    }

    @Override
    public String getExternalId() {
        // 신분 키 → id 누락/비문자열에도 안전하게 처리(카카오 파서와 동일 정책).
        Object id = response.get("id");
        return id == null ? null : String.valueOf(id);
    }

    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getNickname() {
        return (String) response.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) response.get("profile_image");
    }
}
