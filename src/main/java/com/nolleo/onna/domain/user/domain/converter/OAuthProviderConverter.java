package com.nolleo.onna.domain.user.domain.converter;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.user.domain.exception.UserErrorCode;
import com.nolleo.onna.domain.user.domain.model.OAuthProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// [User] OAuthProvider ↔ DB 문자열 변환 — DB는 소문자('google'/'kakao'/'naver') CHECK 제약, Java enum은 대문자.
// @Enumerated(STRING)은 대문자로 저장해 CHECK 위반 → 이 컨버터로 소문자 매핑.
@Converter
public class OAuthProviderConverter implements AttributeConverter<OAuthProvider, String> {

    @Override
    public String convertToDatabaseColumn(OAuthProvider attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public OAuthProvider convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OAuthProvider.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // DB에 enum에 없는 값('gogle' 등)이 들어있는 경우 — 데이터 무결성 위반.
            throw new BusinessException(UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }
}
