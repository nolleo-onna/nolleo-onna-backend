package com.nolleo.onna.domain.map.presentation.controller;

import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.map.application.service.MapQueryService;
import com.nolleo.onna.domain.review.application.service.RatingCacheService;
import com.nolleo.onna.domain.review.presentation.dto.response.RatingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MapController.class)
@WithMockUser
class MapControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MapQueryService mapQueryService;
    @MockBean RatingCacheService ratingCacheService;
    @MockBean JwtProvider jwtProvider;

    @Test
    @DisplayName("GET /map/places/{id}/rating - 200 OK와 평균 평점·리뷰 수를 반환한다")
    void getPlaceRating_returns200WithRating() throws Exception {
        // given
        given(ratingCacheService.getRating(1L)).willReturn(new RatingResponse(4.6, 284));

        // when & then
        mockMvc.perform(get("/api/v1/map/places/1/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("평점 조회 성공"))
                .andExpect(jsonPath("$.data.avgRating").value(4.6))
                .andExpect(jsonPath("$.data.reviewCount").value(284));
    }

    @Test
    @DisplayName("GET /map/places/{id}/rating - 리뷰가 없는 장소는 avgRating 0.0, reviewCount 0을 반환한다")
    void getPlaceRating_returnsZero_whenNoReviews() throws Exception {
        // given
        given(ratingCacheService.getRating(99L)).willReturn(new RatingResponse(0.0, 0));

        // when & then
        mockMvc.perform(get("/api/v1/map/places/99/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avgRating").value(0.0))
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }
}