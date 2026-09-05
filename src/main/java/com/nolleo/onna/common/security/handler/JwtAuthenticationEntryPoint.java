package com.nolleo.onna.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.common.exception.CommonErrorCode;
import com.nolleo.onna.common.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// [Security] 미인증 진입점 — 인증 없이 보호 자원 접근 시 401 JSON(ErrorResponseDto) 응답.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(CommonErrorCode.UNAUTHORIZED.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponseDto.of(CommonErrorCode.UNAUTHORIZED));
    }
}
