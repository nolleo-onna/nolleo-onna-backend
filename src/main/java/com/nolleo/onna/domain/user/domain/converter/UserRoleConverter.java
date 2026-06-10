package com.nolleo.onna.domain.user.domain.converter;

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
        return dbData == null ? null : UserRole.valueOf(dbData.toUpperCase());
    }
}
