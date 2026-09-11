package com.nolleo.onna.domain.event.domain.repository;

import com.nolleo.onna.domain.event.domain.model.Event;

import java.util.List;
import java.util.Optional;

/**
 * [도메인 포트] Event 저장소 인터페이스.
 * 구현체: infrastructure/persistence/EventRepositoryImpl
 */
public interface EventRepository {

    List<Event> findAllActive();

    Optional<Event> findByContentId(String contentId);
}
