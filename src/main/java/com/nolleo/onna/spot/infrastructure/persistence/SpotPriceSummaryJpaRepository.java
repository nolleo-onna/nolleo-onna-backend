package com.nolleo.onna.spot.infrastructure.persistence;

import com.nolleo.onna.spot.domain.entity.SpotPriceSummary;
import com.nolleo.onna.spot.domain.repository.SpotPriceSummaryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotPriceSummaryJpaRepository extends JpaRepository<SpotPriceSummary, String>, SpotPriceSummaryRepository {
}