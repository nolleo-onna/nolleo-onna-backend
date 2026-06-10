package com.nolleo.onna.domain.auth.infrastructure.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.common.security.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.List;

// [Auth] OAuth2 인가요청 저장소 — state는 쿠키(브라우저 바인딩), authReq 본체는 Redis 저장.
@Slf4j(topic = "OAuth2")
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String STATE_COOKIE_NAME = "oauth2_auth_state";
    private static final String REDIS_KEY_PREFIX = "oauth:state:";
    private static final Duration TTL = Duration.ofMinutes(3);
    private static final long COOKIE_MAX_AGE_SECONDS = 180;
    private static final String COOKIE_PATH = "/";

    // GETDEL(Redis 6.2+) 대신 Lua로 GET+DEL을 원자 처리 → 구버전 Redis에서도 single-use 보장.
    private static final DefaultRedisScript<String> GET_DEL_SCRIPT = new DefaultRedisScript<>(
            """
            local v = redis.call('GET', KEYS[1])
            if v then redis.call('DEL', KEYS[1]) end
            return v
            """,
            String.class);

    private final StringRedisTemplate redisTemplate;
    private final CookieProperties cookieProperties;
    private final ObjectMapper objectMapper;

    public HttpCookieOAuth2AuthorizationRequestRepository(StringRedisTemplate redisTemplate,
                                                          CookieProperties cookieProperties) {
        this.redisTemplate = redisTemplate;
        this.cookieProperties = cookieProperties;
        this.objectMapper = buildObjectMapper();
    }

    // OAuth2AuthorizationRequest JSON 직렬화용 ObjectMapper 구성(Security mixin 등록).
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        ClassLoader loader = getClass().getClassLoader();
        mapper.registerModules(SecurityJackson2Modules.getModules(loader));
        mapper.registerModule(new OAuth2ClientJackson2Module());
        return mapper;
    }

    // 쿠키의 state로 Redis에서 authReq 복원(없으면 null).
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = readStateCookie(request);
        if (state == null) {
            return null;
        }
        String json = redisTemplate.opsForValue().get(redisKey(state));
        return json == null ? null : deserialize(json);
    }

    // 로그인 시작 시 authReq를 Redis에 저장하고 state를 쿠키로 내려줌(null이면 쿠키 만료).
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            String state = readStateCookie(request);
            if (state != null) {
                redisTemplate.delete(redisKey(state));
            }
            expireStateCookie(response);
            return;
        }
        String state = authorizationRequest.getState();
        redisTemplate.opsForValue().set(redisKey(state), serialize(authorizationRequest), TTL);
        addStateCookie(response, state);
    }

    // 콜백 시 authReq를 Lua GET + DEL로 꺼내며 삭제(single-use)하고 state 쿠키도 만료.
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        String state = readStateCookie(request);
        if (state == null) {
            return null;
        }
        String json = redisTemplate.execute(GET_DEL_SCRIPT, List.of(redisKey(state)));
        expireStateCookie(response);
        return json == null ? null : deserialize(json);
    }

    // 요청에서 state 쿠키 값 추출(없으면 null).
    private String readStateCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, STATE_COOKIE_NAME);
        return cookie == null ? null : cookie.getValue();
    }

    // state 쿠키 발급(Max-Age 180초).
    private void addStateCookie(HttpServletResponse response, String state) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                baseCookie(state).maxAge(COOKIE_MAX_AGE_SECONDS).build().toString());
    }

    // state 쿠키 즉시 만료(Max-Age 0).
    private void expireStateCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                baseCookie("").maxAge(0).build().toString());
    }

    // state 쿠키 공통 속성 빌더(HttpOnly, Secure, SameSite=Lax, host-only).
    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        // SameSite는 provider→콜백 top-level 리다이렉트에서 쿠키가 살아남아야 하므로 Lax 고정.
        // Domain은 의도적으로 미지정(host-only).
        return ResponseCookie.from(STATE_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }

    // state → Redis 키("oauth:state:{state}") 변환.
    private String redisKey(String state) {
        return REDIS_KEY_PREFIX + state;
    }

    // authReq → JSON 문자열(실패 시 IllegalStateException).
    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            return objectMapper.writeValueAsString(authorizationRequest);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OAuth2AuthorizationRequest 직렬화 실패", e);
        }
    }

    // JSON 문자열 → authReq(손상/구버전 값이면 null).
    private OAuth2AuthorizationRequest deserialize(String json) {
        try {
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (JsonProcessingException e) {
            // 손상/구버전 값 → authReq 없음으로 처리(Spring이 흐름 거부 → 재로그인 유도). 장애 추적용 로그.
            log.debug("OAuth2AuthorizationRequest 역직렬화 실패 - 흐름 무효 처리", e);
            return null;
        }
    }
}