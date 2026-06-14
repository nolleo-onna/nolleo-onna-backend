package com.nolleo.onna.domain.spot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SpotJpaRepository extends JpaRepository<SpotEntity, String> {

    @Query("SELECT s FROM SpotEntity s WHERE s.active = true")
    List<SpotEntity> findAllActive();
}