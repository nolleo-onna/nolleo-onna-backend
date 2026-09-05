package com.nolleo.onna.domain.spot.presentation.controller;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.spot.application.service.SpotQueryService;
import com.nolleo.onna.domain.spot.domain.exception.SpotErrorCode;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotDetailResponse;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotMarkerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpotController.class)
@WithMockUser
class SpotControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean SpotQueryService spotQueryService;
    @MockBean JwtProvider jwtProvider;

    @Test
    @DisplayName("GET /api/spots/markers - 200 OK와 마커 목록을 반환한다")
    void getSpotMarkers_returns200WithMarkerList() throws Exception {
        // given
        List<SpotMarkerResponse> markers = List.of(
                new SpotMarkerResponse(
                        "1234567890", "12", "해운대 해수욕장",
                        new BigDecimal("129.1603387"), new BigDecimal("35.1588473"),
                        "https://example.com/img.jpg", "관광지", null, null
                )
        );
        given(spotQueryService.getSpotMarkers()).willReturn(markers);

        // when & then
        mockMvc.perform(get("/api/v1/spots/markers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("장소 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].contentId").value("1234567890"))
                .andExpect(jsonPath("$.data[0].title").value("해운대 해수욕장"))
                .andExpect(jsonPath("$.data[0].mapX").value(129.1603387));
    }

    @Test
    @DisplayName("GET /api/spots/markers - 활성 장소가 없으면 빈 배열을 반환한다")
    void getSpotMarkers_returnsEmptyArray_whenNoActiveSpots() throws Exception {
        // given
        given(spotQueryService.getSpotMarkers()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/spots/markers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /api/spots/{contentId} - 200 OK와 상세 정보를 반환한다")
    void getSpotDetail_returns200WithDetail() throws Exception {
        // given
        String contentId = "1234567890";
        SpotDetailResponse detail = new SpotDetailResponse(
                contentId, "12", "해운대 해수욕장",
                new BigDecimal("129.1603387"), new BigDecimal("35.1588473"),
                "https://example.com/img.jpg", null,
                "관광지", null, null, "26", "26350",
                "051-000-0000", null, "부산광역시 해운대구", null, "48094",
                "해운대 해수욕장 소개글", null, true,
                List.of(),
                null, null, null, null
        );
        given(spotQueryService.getSpotDetail(contentId)).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/v1/spots/{contentId}", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("장소 상세 조회 성공"))
                .andExpect(jsonPath("$.data.contentId").value(contentId))
                .andExpect(jsonPath("$.data.title").value("해운대 해수욕장"))
                .andExpect(jsonPath("$.data.addr1").value("부산광역시 해운대구"))
                .andExpect(jsonPath("$.data.tel").value("051-000-0000"));
    }

    @Test
    @DisplayName("GET /api/spots/{contentId} - 존재하지 않는 장소 조회 시 404를 반환한다")
    void getSpotDetail_returns404_whenSpotNotFound() throws Exception {
        // given
        given(spotQueryService.getSpotDetail("not-exist"))
                .willThrow(new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/spots/{contentId}", "not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("SPOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다"));
    }
}
