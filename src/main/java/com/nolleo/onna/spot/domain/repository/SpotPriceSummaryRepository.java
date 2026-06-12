package com.nolleo.onna.spot.domain.repository;

import com.nolleo.onna.spot.domain.entity.SpotPriceSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotPriceSummaryRepository extends JpaRepository<SpotPriceSummary, String> {
}