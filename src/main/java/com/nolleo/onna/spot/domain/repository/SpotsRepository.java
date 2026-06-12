package com.nolleo.onna.spot.domain.repository;

import com.nolleo.onna.spot.domain.entity.Spots;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotsRepository extends JpaRepository<Spots, String> {
}