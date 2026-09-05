package com.nolleo.onna.common.config;

import com.nolleo.onna.common.security.handler.JwtAccessDeniedHandler;
import com.nolleo.onna.common.security.handler.JwtAuthenticationEntryPoint;
import com.nolleo.onna.common.security.jwt.JwtAuthenticationFilter;
import com.nolleo.onna.domain.auth.infrastructure.oauth.CustomOAuth2UserService;
import com.nolleo.onna.domain.auth.infrastructure.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.nolleo.onna.domain.auth.infrastructure.oauth.OAuth2FailureHandler;
import com.nolleo.onna.domain.auth.infrastructure.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// [Common] Spring Security 설정 — JWT 인증 필터 + OAuth2 로그인 + 401/403 예외핸들러 연결.
// STATELESS: 세션 미사용(JWT). CORS: 프론트와 쿠키 주고받으려면 allowCredentials 필요.
// 권한 정책: refresh/oauth/swagger/actuator는 permitAll, me/logout 포함 나머지는 인증 필요.
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger / Actuator
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // health만 공개(LB/k8s 프로브용). env/heapdump/configprops 등 나머지는 인증 필요 → 시크릿 노출 방지.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // OAuth 로그인 시작·콜백
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // 지도·장소 조회 — 비로그인 사용자도 접근 가능
                        .requestMatchers("/api/v1/map/**").permitAll()
                        .requestMatchers("/api/v1/spots/**").permitAll()
                        .requestMatchers("/api/v1/food/**").permitAll()
                        // 날씨·혼잡도 — 비로그인 사용자도 접근 가능
                        .requestMatchers("/api/v1/weather/**").permitAll()
                        .requestMatchers("/api/v1/congestion/**").permitAll()
                        // 재발급은 access 없이 refresh 쿠키로 동작 → permitAll
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        // me/logout 등 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestRepository(authorizationRequestRepository))
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키 송수신 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
