package com.nolleo.onna.domain.spot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpotImageJpaRepository extends JpaRepository<SpotImageEntity, Long> {

    List<SpotImageEntity> findByContentId(String contentId);
}