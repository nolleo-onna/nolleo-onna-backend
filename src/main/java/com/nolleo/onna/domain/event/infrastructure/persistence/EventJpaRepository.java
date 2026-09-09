package com.nolleo.onna.domain.event.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventJpaRepository extends JpaRepository<EventEntity, String> {

    @Query("SELECT e FROM EventEntity e WHERE e.active = true")
    List<EventEntity> findAllActive();

    Optional<EventEntity> findByContentIdAndActiveTrue(String contentId);
}
