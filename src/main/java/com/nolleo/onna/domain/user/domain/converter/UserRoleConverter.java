package com.nolleo.onna.domain.user.domain.converter;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.user.domain.exception.UserErrorCode;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// [User] UserRole ↔ DB 문자열 변환 — DB는 소문자('user'/'admin') CHECK 제약, Java enum은 대문자.
@Converter
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return UserRole.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // DB에 enum에 없는 값이 들어있는 경우 — 데이터 무결성 위반.
            throw new BusinessException(UserErrorCode.UNSUPPORTED_USER_ROLE);
        }
    }
}
