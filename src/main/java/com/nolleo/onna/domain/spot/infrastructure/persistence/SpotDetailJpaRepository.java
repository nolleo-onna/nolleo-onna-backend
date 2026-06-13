package com.nolleo.onna.domain.spot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotDetailJpaRepository extends JpaRepository<SpotDetailEntity, String> {
}