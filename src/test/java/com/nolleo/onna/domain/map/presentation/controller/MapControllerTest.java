package com.nolleo.onna.domain.map.presentation.controller;

import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.map.application.service.MapQueryService;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import com.nolleo.onna.domain.map.presentation.dto.response.MapPlaceResponse;
import com.nolleo.onna.domain.review.application.service.RatingCacheService;
import com.nolleo.onna.domain.review.presentation.dto.response.RatingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    private MapPlaceResponse sampleResponse() {
        MapPlace place = new MapPlace(
                1L, PlaceType.SPOT, "orig-1", "해운대 해수욕장", "해운대구", PlaceCategory.VE,
                new BigDecimal("129.160"), new BigDecimal("35.158"),
                "https://example.com/img.jpg", null, true, true,
                new BigDecimal("4.30"), 10L, 0L
        );
        return MapPlaceResponse.from(place);
    }

    @Test
    @DisplayName("GET /map/places - keyword 없이 요청하면 200 OK와 장소 목록을 반환한다")
    void getMapPlaces_withoutKeyword_returns200() throws Exception {
        given(mapQueryService.getMapPlaces(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/map/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("지도 장소 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content[0].name").value("해운대 해수욕장"));
    }

    @Test
    @DisplayName("GET /map/places?keyword=해운대 - keyword를 전달하면 서비스에 그대로 전달된다")
    void getMapPlaces_withKeyword_passesKeywordToService() throws Exception {
        given(mapQueryService.getMapPlaces(isNull(), isNull(), isNull(), eq("해운대"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/map/places").param("keyword", "해운대"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("해운대 해수욕장"));
    }

    @Test
    @DisplayName("GET /map/places?keyword= - 빈 keyword는 서비스에 빈 문자열로 전달된다")
    void getMapPlaces_withBlankKeyword_returns200() throws Exception {
        given(mapQueryService.getMapPlaces(isNull(), isNull(), isNull(), eq(""), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/map/places").param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("해운대 해수욕장"));
    }

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