package com.nolleo.onna.spot.domain.repository;

import com.nolleo.onna.spot.domain.entity.SpotPriceSummary;

import java.util.Optional;

public interface SpotPriceSummaryRepository {

    Optional<SpotPriceSummary> findById(String spotContentId);
}