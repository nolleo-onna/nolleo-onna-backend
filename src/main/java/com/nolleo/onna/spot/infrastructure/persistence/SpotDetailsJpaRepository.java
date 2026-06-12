package com.nolleo.onna.spot.infrastructure.persistence;

import com.nolleo.onna.spot.domain.entity.SpotDetails;
import com.nolleo.onna.spot.domain.repository.SpotDetailsRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotDetailsJpaRepository extends JpaRepository<SpotDetails, String>, SpotDetailsRepository {
}