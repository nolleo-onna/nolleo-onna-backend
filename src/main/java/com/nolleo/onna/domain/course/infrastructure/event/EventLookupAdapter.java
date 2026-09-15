package com.nolleo.onna.domain.course.infrastructure.event;

import com.nolleo.onna.domain.course.application.dto.EventCandidate;
import com.nolleo.onna.domain.course.application.port.EventLookupPort;
import com.nolleo.onna.domain.event.domain.model.Event;
import com.nolleo.onna.domain.event.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * EventLookupPort의 어댑터. Event 컨텍스트의 도메인 모델을 Course 컨텍스트가 이해하는 EventCandidate로 변환한다.
 * "아직 끝나지 않음"의 기준 날짜는 KST 오늘이다.
 */
@Component
@RequiredArgsConstructor
public class EventLookupAdapter implements EventLookupPort {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final EventRepository eventRepository;

    @Override
    public List<EventCandidate> findUpcomingByTitle(String title, int limit) {
        // 정렬(정확 일치 → 시작일)은 DB가 끝내므로 순서를 그대로 넘긴다
        return eventRepository.findActiveByTitleNotEnded(title, LocalDate.now(KST), limit).stream()
                .map(EventLookupAdapter::toCandidate)
                .toList();
    }

    private static EventCandidate toCandidate(Event event) {
        return new EventCandidate(
                event.getContentId(), event.getTitle(),
                event.getMapX(), event.getMapY(),
                event.getEventStartDate(), event.getEventEndDate(),
                event.getEventPlace());
    }
}
