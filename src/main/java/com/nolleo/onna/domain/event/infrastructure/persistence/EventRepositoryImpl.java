package com.nolleo.onna.domain.event.infrastructure.persistence;

import com.nolleo.onna.domain.event.domain.model.Event;
import com.nolleo.onna.domain.event.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    @Override
    public List<Event> findActiveByTitleNotEnded(String title, LocalDate today, int limit) {
        String compact = title == null ? "" : title.replace(" ", "").strip();
        if (compact.isEmpty() || limit <= 0) return List.of();
        return jpaRepository.findActiveByTitleNotEnded(escapeLike(compact), today, limit).stream()
                .map(EventEntity::toDomain)
                .toList();
    }

    /** LIKE 와일드카드가 사용자 입력에 섞여 들어와도 리터럴로 비교되게 이스케이프한다 (PostgreSQL 기본 이스케이프 문자는 \) */
    private static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
