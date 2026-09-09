package com.nolleo.onna.domain.user.presentation.controller;

import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.favorite.application.service.FavoriteQueryService;
import com.nolleo.onna.domain.favorite.domain.model.FavoritePeriodType;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatsResponse;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean FavoriteQueryService favoriteQueryService;
    @MockBean JwtProvider jwtProvider;

    private Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorite-stats - 오늘 찜이 있으면 TODAY 통계와 200을 반환한다")
    void getFavoriteStats_returns200_withTodayStats() throws Exception {
        given(favoriteQueryService.getStats(1L))
                .willReturn(new FavoriteStatsResponse(FavoritePeriodType.TODAY, 3L, "오늘 3개 찜했어요!"));

        mockMvc.perform(get("/api/v1/users/me/favorite-stats")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("찜 통계 조회 성공"))
                .andExpect(jsonPath("$.data.period").value("TODAY"))
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.message").value("오늘 3개 찜했어요!"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorite-stats - 오늘 찜이 없고 이번 주 찜이 있으면 WEEK 통계와 200을 반환한다")
    void getFavoriteStats_returns200_withWeekStats() throws Exception {
        given(favoriteQueryService.getStats(1L))
                .willReturn(new FavoriteStatsResponse(FavoritePeriodType.WEEK, 5L, "이번 주 5개 찜했어요!"));

        mockMvc.perform(get("/api/v1/users/me/favorite-stats")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.count").value(5))
                .andExpect(jsonPath("$.data.message").value("이번 주 5개 찜했어요!"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorite-stats - 이번 달 찜이 있으면 MONTH 통계와 200을 반환한다")
    void getFavoriteStats_returns200_withMonthStats() throws Exception {
        given(favoriteQueryService.getStats(1L))
                .willReturn(new FavoriteStatsResponse(FavoritePeriodType.MONTH, 12L, "이번 달 12개 찜했어요!"));

        mockMvc.perform(get("/api/v1/users/me/favorite-stats")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("MONTH"))
                .andExpect(jsonPath("$.data.count").value(12))
                .andExpect(jsonPath("$.data.message").value("이번 달 12개 찜했어요!"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorite-stats - 이번 달 찜이 없으면 count=0과 특별 메시지를 반환한다")
    void getFavoriteStats_returns200_withZeroMonthStats() throws Exception {
        given(favoriteQueryService.getStats(1L))
                .willReturn(new FavoriteStatsResponse(FavoritePeriodType.MONTH, 0L, "이번 달에 찜한 장소가 없어요"));

        mockMvc.perform(get("/api/v1/users/me/favorite-stats")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("MONTH"))
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.message").value("이번 달에 찜한 장소가 없어요"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorite-stats - 인증되지 않은 요청은 인증을 요구한다")
    void getFavoriteStats_requiresAuthentication_whenNotAuthenticated() throws Exception {
        // @WebMvcTest 환경에서는 JwtAuthenticationEntryPoint 대신 기본 Security 동작(302)이 적용됨
        mockMvc.perform(get("/api/v1/users/me/favorite-stats"))
                .andExpect(status().is3xxRedirection());
    }
}
