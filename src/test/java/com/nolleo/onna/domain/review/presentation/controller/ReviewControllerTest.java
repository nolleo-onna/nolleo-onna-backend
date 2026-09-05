package com.nolleo.onna.domain.review.presentation.controller;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.review.application.service.ReviewCommandService;
import com.nolleo.onna.domain.review.domain.exception.ReviewErrorCode;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ReviewCommandService reviewCommandService;
    @MockBean JwtProvider jwtProvider;

    private Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("POST /reviews - 유효한 요청으로 리뷰 등록 시 201을 반환한다")
    void createReview_returns201_whenValidRequest() throws Exception {
        // given
        willDoNothing().given(reviewCommandService).createReview(anyLong(), anyLong(), anyInt());

        // when & then
        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mapPlaceId": 10,
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("리뷰 등록 성공"));
    }

    @Test
    @DisplayName("POST /reviews - rating이 1 미만이면 400을 반환한다")
    void createReview_returns400_whenRatingTooLow() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mapPlaceId": 10,
                                  "rating": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /reviews - rating이 5 초과이면 400을 반환한다")
    void createReview_returns400_whenRatingTooHigh() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mapPlaceId": 10,
                                  "rating": 6
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /reviews - mapPlaceId가 없으면 400을 반환한다")
    void createReview_returns400_whenMapPlaceIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /reviews - 존재하지 않는 mapPlaceId로 요청 시 404를 반환한다")
    void createReview_returns404_whenMapPlaceNotFound() throws Exception {
        // given
        willThrow(new BusinessException(ReviewErrorCode.MAP_PLACE_NOT_FOUND))
                .given(reviewCommandService).createReview(anyLong(), anyLong(), anyInt());

        // when & then
        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mapPlaceId": 999,
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("R001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 장소입니다."));
    }
}