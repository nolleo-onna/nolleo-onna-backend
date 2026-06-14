package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.entity.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.repository.SpotPriceSummaryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotPriceSummaryJpaRepository extends JpaRepository<SpotPriceSummary, String>, SpotPriceSummaryRepository {
}