package com.nolleo.onna.spot.domain.repository;

import com.nolleo.onna.spot.domain.entity.SpotDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotDetailsRepository extends JpaRepository<SpotDetails, String> {
}