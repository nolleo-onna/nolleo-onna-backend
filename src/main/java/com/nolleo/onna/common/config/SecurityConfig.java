// [Common] Spring Security 기본 설정 — JWT 인증 필터는 추후 추가.
// 현재는 Swagger, Actuator, 인증 엔드포인트만 허용하고 나머지는 인증 필요.
package com.nolleo.onna.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // 인증 엔드포인트 (추후 추가)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}