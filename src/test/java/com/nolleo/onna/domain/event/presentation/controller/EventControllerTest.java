package com.nolleo.onna.domain.event.presentation.controller;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.event.application.dto.EventDetailResult;
import com.nolleo.onna.domain.event.application.service.EventQueryService;
import com.nolleo.onna.domain.event.domain.exception.EventErrorCode;
import com.nolleo.onna.domain.event.domain.model.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false) // 행사 조회는 비로그인 접근 허용 — 인증 필터 없이 테스트
class EventControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean EventQueryService eventQueryService;
    @MockBean JwtProvider jwtProvider;

    @Test
    @DisplayName("GET /api/v1/events - 200 OK와 행사 목록을 반환한다")
    void getEvents_returns200WithEventList() throws Exception {
        // given
        List<EventDetailResult> events = List.of(buildEventDetailResult("2991394"));
        given(eventQueryService.getEvents()).willReturn(events);

        // when & then
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("행사 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].contentId").value("2991394"))
                .andExpect(jsonPath("$.data[0].title").value("2026 부산나이트워크42K with dsec"))
                .andExpect(jsonPath("$.data[0].eventPlace").value("APEC나루공원"));
    }

    @Test
    @DisplayName("GET /api/v1/events - 활성 행사가 없으면 빈 배열을 반환한다")
    void getEvents_returnsEmptyArray_whenNoActiveEvents() throws Exception {
        // given
        given(eventQueryService.getEvents()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/events/{contentId} - 200 OK와 행사 상세 정보를 반환한다")
    void getEventDetail_returns200WithDetail() throws Exception {
        // given
        String contentId = "2991394";
        given(eventQueryService.getEventDetail(contentId)).willReturn(buildEventDetailResult(contentId));

        // when & then
        mockMvc.perform(get("/api/v1/events/{contentId}", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("행사 상세 조회 성공"))
                .andExpect(jsonPath("$.data.contentId").value(contentId))
                .andExpect(jsonPath("$.data.title").value("2026 부산나이트워크42K with dsec"))
                .andExpect(jsonPath("$.data.addr1").value("부산광역시 해운대구 수영강변대로 85 (우동)"))
                .andExpect(jsonPath("$.data.sponsor1").value("부산일보사, 어반씨앤에스"));
    }

    @Test
    @DisplayName("GET /api/v1/events/{contentId} - 존재하지 않는 행사 조회 시 404를 반환한다")
    void getEventDetail_returns404_whenEventNotFound() throws Exception {
        // given
        given(eventQueryService.getEventDetail("not-exist"))
                .willThrow(new BusinessException(EventErrorCode.EVENT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/events/{contentId}", "not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("행사를 찾을 수 없습니다"));
    }

    private EventDetailResult buildEventDetailResult(String contentId) {
        Event event = mock(Event.class);
        given(event.getContentId()).willReturn(contentId);
        given(event.getTitle()).willReturn("2026 부산나이트워크42K with dsec");
        given(event.getEventStartDate()).willReturn(LocalDate.of(2026, 8, 29));
        given(event.getEventEndDate()).willReturn(LocalDate.of(2026, 8, 30));
        given(event.getMapX()).willReturn(new BigDecimal("129.1273173"));
        given(event.getMapY()).willReturn(new BigDecimal("35.1679082"));
        given(event.getTel()).willReturn("070-4705-2008");
        given(event.getAddr1()).willReturn("부산광역시 해운대구 수영강변대로 85 (우동)");
        given(event.getAddr2()).willReturn(null);
        given(event.getFirstImage()).willReturn("https://tong.visitkorea.or.kr/cms/resource/37/4069137_image2_1.jpg");
        given(event.getFirstImage2()).willReturn("https://tong.visitkorea.or.kr/cms/resource/37/4069137_image3_1.jpg");
        given(event.getEventPlace()).willReturn("APEC나루공원");
        given(event.getPlayTime()).willReturn("16:00~07:00");
        given(event.getUseTimeFestival()).willReturn("8K 38,000원");
        given(event.getSponsor1()).willReturn("부산일보사, 어반씨앤에스");
        given(event.getSponsor1Tel()).willReturn("070-4705-2008");
        given(event.getSponsor2()).willReturn("㈜블렌트");
        given(event.getSponsor2Tel()).willReturn("070-4705-2008");
        given(event.getAgeLimit()).willReturn(null);
        given(event.getEventHomepage()).willReturn(null);
        return new EventDetailResult(event);
    }
}
