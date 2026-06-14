package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.entity.Spots;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotsJpaRepository extends JpaRepository<Spots, String>, SpotsRepository {
}