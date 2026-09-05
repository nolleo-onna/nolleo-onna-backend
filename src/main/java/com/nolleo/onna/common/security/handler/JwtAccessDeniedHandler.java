package com.nolleo.onna.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.common.exception.CommonErrorCode;
import com.nolleo.onna.common.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// [Security] 권한 부족 핸들러 — 인증은 됐으나 role 부족 시 403 JSON(ErrorResponseDto) 응답.
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(CommonErrorCode.FORBIDDEN.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponseDto.of(CommonErrorCode.FORBIDDEN));
    }
}
