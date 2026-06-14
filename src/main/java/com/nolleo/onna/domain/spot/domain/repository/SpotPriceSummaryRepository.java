package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.entity.SpotPriceSummary;

import java.util.Optional;

public interface SpotPriceSummaryRepository {

    Optional<SpotPriceSummary> findById(String spotContentId);
}