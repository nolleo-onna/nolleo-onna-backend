package com.nolleo.onna.domain.course.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ShareTokenGeneratorTest {

    @Test
    @DisplayName("토큰은 32자이고 URL-safe 문자(영숫자, -, _)로만 이루어진다 — share_token VARCHAR(64) 안에 들어간다")
    void generate_isUrlSafe32Chars() {
        String token = ShareTokenGenerator.generate();

        assertThat(token).hasSize(ShareTokenGenerator.TOKEN_LENGTH);
        assertThat(token).matches("[A-Za-z0-9_-]{32}");
        assertThat(token).doesNotContain("=", "+", "/");
    }

    @Test
    @DisplayName("1,000회 생성해도 중복이 없다")
    void generate_isUnique() {
        Set<String> tokens = new HashSet<>();
        IntStream.range(0, 1_000).forEach(i -> tokens.add(ShareTokenGenerator.generate()));

        assertThat(tokens).hasSize(1_000);
    }
}
