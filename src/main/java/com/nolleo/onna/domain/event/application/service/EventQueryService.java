package com.nolleo.onna.domain.event.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.event.application.dto.EventDetailResult;
import com.nolleo.onna.domain.event.domain.exception.EventErrorCode;
import com.nolleo.onna.domain.event.domain.model.Event;
import com.nolleo.onna.domain.event.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService {

    private final EventRepository eventRepository;

    public List<EventDetailResult> getEvents() {
        return eventRepository.findAllActive().stream()
                .map(EventDetailResult::new)
                .toList();
    }

    public EventDetailResult getEventDetail(String contentId) {
        Event event = eventRepository.findByContentId(contentId)
                .orElseThrow(() -> new BusinessException(EventErrorCode.EVENT_NOT_FOUND));
        return new EventDetailResult(event);
    }
}
