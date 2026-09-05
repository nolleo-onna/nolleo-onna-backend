// [Auth] OAuth 토큰 교환 후 호출 — provider 유저정보를 파싱해 User upsert 후 OAuth2UserPrincipal 반환.
// 실제 JWT 발급은 SuccessHandler에서. 여기선 provider 정보 → DB 동기화까지만 책임.
// registrationId로 provider 판별 → OAuth2UserInfoFactory 파싱 → UserService.oauthLogin upsert.
package com.nolleo.onna.domain.auth.infrastructure.oauth;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.auth.domain.exception.AuthErrorCode;
import com.nolleo.onna.domain.user.application.UserService;
import com.nolleo.onna.domain.user.domain.model.OAuthProvider;
import com.nolleo.onna.domain.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j(topic = "OAuth2")
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest); // provider userinfo 조회
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        try {
            OAuthProvider provider = OAuthProvider.from(registrationId);
            OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(provider, oAuth2User.getAttributes());

            String externalId = userInfo.getExternalId();
            if (externalId == null || externalId.isBlank() || "null".equalsIgnoreCase(externalId)) {
                throw new BusinessException(AuthErrorCode.OAUTH_PROCESSING_FAILED);
            }

            User user = userService.oauthLogin(
                    externalId,
                    provider,
                    userInfo.getEmail(),
                    resolveNickname(userInfo),
                    userInfo.getProfileImageUrl());

            return new OAuth2UserPrincipal(user.getId(), user.getRole(), oAuth2User.getAttributes());
        } catch (BusinessException e) {
            throw oauthException(e);
        } catch (Exception e) {
            // DB 오류 등 예기치 못한 예외 → 실패 핸들러로 흐르도록 OAuth 예외로 래핑 + 장애 추적 로그.
            log.error("OAuth 유저 처리 실패 - provider: {}", registrationId, e);
            throw oauthException(new BusinessException(AuthErrorCode.OAUTH_PROCESSING_FAILED));
        }
    }

    // 닉네임 미동의/누락 시 임시 닉네임으로 대체(엔티티 nickname not-null 방어).
    private String resolveNickname(OAuth2UserInfo userInfo) {
        String nickname = userInfo.getNickname();
        return (nickname == null || nickname.isBlank()) ? "놀러온나" : nickname;
    }

    private OAuth2AuthenticationException oauthException(BusinessException e) {
        OAuth2Error error = new OAuth2Error(
                e.getErrorCode().getErrorCode(),
                e.getErrorCode().getMessage(),
                null);
        return new OAuth2AuthenticationException(error, e);
    }
}
