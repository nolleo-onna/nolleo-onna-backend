package com.nolleo.onna.domain.event.infrastructure.persistence;

import com.nolleo.onna.domain.event.domain.model.Event;
import com.nolleo.onna.domain.event.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** [인프라 어댑터] EventRepository 도메인 포트의 JPA 구현체. */
@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepository {

    private final EventJpaRepository jpaRepository;

    @Override
    public List<Event> findAllActive() {
        return jpaRepository.findAllActive().stream().map(EventEntity::toDomain).toList();
    }

    @Override
    public Optional<Event> findByContentId(String contentId) {
        return jpaRepository.findByContentIdAndActiveTrue(contentId).map(EventEntity::toDomain);
    }
}
