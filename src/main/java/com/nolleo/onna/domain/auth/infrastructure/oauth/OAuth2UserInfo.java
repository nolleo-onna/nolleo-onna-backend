// [Auth] provider별 유저정보 파싱 추상 — google/kakao/naver의 응답 구조가 제각각이라 공통 인터페이스로 통일.
// 구현체: GoogleOAuth2UserInfo / KakaoOAuth2UserInfo / NaverOAuth2UserInfo.
package com.nolleo.onna.domain.auth.infrastructure.oauth;

public interface OAuth2UserInfo {

    String getExternalId();      // provider 내 유저 고유 ID (DB external_id)

    String getEmail();

    String getNickname();

    String getProfileImageUrl();
}
