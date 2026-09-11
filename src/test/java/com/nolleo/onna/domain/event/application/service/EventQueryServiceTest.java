package com.nolleo.onna.domain.event.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.event.application.dto.EventDetailResult;
import com.nolleo.onna.domain.event.domain.exception.EventErrorCode;
import com.nolleo.onna.domain.event.domain.model.Event;
import com.nolleo.onna.domain.event.domain.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

    @Mock EventRepository eventRepository;

    @InjectMocks EventQueryService eventQueryService;

    @Test
    @DisplayName("활성 행사 목록을 Result 리스트로 반환한다")
    void getEvents_returnsActiveEventsAsResultList() {
        // given
        Event mockEvent = buildMockEvent("2991394");
        given(eventRepository.findAllActive()).willReturn(List.of(mockEvent));

        // when
        List<EventDetailResult> result = eventQueryService.getEvents();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).event().getContentId()).isEqualTo("2991394");
        assertThat(result.get(0).event().getTitle()).isEqualTo("2026 부산나이트워크42K with dsec");
        assertThat(result.get(0).event().getEventPlace()).isEqualTo("APEC나루공원");
    }

    @Test
    @DisplayName("활성 행사가 없으면 빈 목록을 반환한다")
    void getEvents_returnsEmptyList_whenNoActiveEvents() {
        // given
        given(eventRepository.findAllActive()).willReturn(List.of());

        // when
        List<EventDetailResult> result = eventQueryService.getEvents();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("contentId로 행사 상세 정보를 반환한다")
    void getEventDetail_returnsDetail_whenEventFound() {
        // given
        String contentId = "2991394";
        Event mockEvent = buildMockEvent(contentId);
        given(eventRepository.findByContentId(contentId)).willReturn(Optional.of(mockEvent));

        // when
        EventDetailResult result = eventQueryService.getEventDetail(contentId);

        // then
        assertThat(result.event().getContentId()).isEqualTo(contentId);
        assertThat(result.event().getTitle()).isEqualTo("2026 부산나이트워크42K with dsec");
        assertThat(result.event().getEventStartDate()).isEqualTo(LocalDate.of(2026, 8, 29));
        assertThat(result.event().getEventEndDate()).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(result.event().getAddr1()).isEqualTo("부산광역시 해운대구 수영강변대로 85 (우동)");
        assertThat(result.event().getEventPlace()).isEqualTo("APEC나루공원");
        assertThat(result.event().getSponsor1()).isEqualTo("부산일보사, 어반씨앤에스");
    }

    @Test
    @DisplayName("존재하지 않는 contentId 조회 시 EVENT_NOT_FOUND 예외를 던진다")
    void getEventDetail_throwsException_whenEventNotFound() {
        // given
        given(eventRepository.findByContentId("not-exist")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventQueryService.getEventDetail("not-exist"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(EventErrorCode.EVENT_NOT_FOUND));
    }

    private Event buildMockEvent(String contentId) {
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
        return event;
    }
}
