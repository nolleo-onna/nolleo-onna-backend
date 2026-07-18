package com.nolleo.onna.domain.favorite.presentation.controller;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.favorite.application.service.FavoriteCommandService;
import com.nolleo.onna.domain.favorite.application.service.FavoriteQueryService;
import com.nolleo.onna.domain.favorite.domain.exception.FavoriteErrorCode;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteItemResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatusResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteToggleResponse;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
class FavoriteControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean FavoriteCommandService favoriteCommandService;
    @MockBean FavoriteQueryService favoriteQueryService;
    @MockBean JwtProvider jwtProvider;

    private Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // ── toggle ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/favorites/{mapPlaceId}/toggle - 찜 추가 시 favorited=true와 200을 반환한다")
    void toggle_returns200_whenFavoriteAdded() throws Exception {
        given(favoriteCommandService.toggle(1L, 10L))
                .willReturn(new FavoriteToggleResponse(10L, true));

        mockMvc.perform(post("/api/v1/favorites/10/toggle")
                        .with(authentication(authAs(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("찜 추가 성공"))
                .andExpect(jsonPath("$.data.mapPlaceId").value(10))
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{mapPlaceId}/toggle - 찜 취소 시 favorited=false와 200을 반환한다")
    void toggle_returns200_whenFavoriteCanceled() throws Exception {
        given(favoriteCommandService.toggle(1L, 10L))
                .willReturn(new FavoriteToggleResponse(10L, false));

        mockMvc.perform(post("/api/v1/favorites/10/toggle")
                        .with(authentication(authAs(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("찜 취소 성공"))
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{mapPlaceId}/toggle - 존재하지 않는 장소 요청 시 404를 반환한다")
    void toggle_returns404_whenMapPlaceNotFound() throws Exception {
        willThrow(new BusinessException(FavoriteErrorCode.MAP_PLACE_NOT_FOUND))
                .given(favoriteCommandService).toggle(anyLong(), anyLong());

        mockMvc.perform(post("/api/v1/favorites/999/toggle")
                        .with(authentication(authAs(1L)))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("F001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 장소입니다."));
    }

    // ── getFavorites ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/favorites - 찜 목록을 정상적으로 조회하고 200을 반환한다")
    void getFavorites_returns200_withPagedResult() throws Exception {
        FavoriteItemResponse item = new FavoriteItemResponse(
                10L, "테스트 장소", "마포구", "AT4",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(126.9), OffsetDateTime.now()
        );

        given(favoriteQueryService.getFavorites(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/v1/favorites")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("찜 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content[0].mapPlaceId").value(10))
                .andExpect(jsonPath("$.data.content[0].name").value("테스트 장소"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ── getStatus ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/favorites/{mapPlaceId}/status - 찜 여부를 정상적으로 반환하고 200을 반환한다")
    void getStatus_returns200_withFavoriteStatus() throws Exception {
        given(favoriteQueryService.getStatus(1L, 10L))
                .willReturn(new FavoriteStatusResponse(10L, true));

        mockMvc.perform(get("/api/v1/favorites/10/status")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("찜 여부 조회 성공"))
                .andExpect(jsonPath("$.data.mapPlaceId").value(10))
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/favorites/{mapPlaceId}/status - 존재하지 않는 장소 요청 시 404를 반환한다")
    void getStatus_returns404_whenMapPlaceNotFound() throws Exception {
        willThrow(new BusinessException(FavoriteErrorCode.MAP_PLACE_NOT_FOUND))
                .given(favoriteQueryService).getStatus(anyLong(), anyLong());

        mockMvc.perform(get("/api/v1/favorites/999/status")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("F001"));
    }
}
