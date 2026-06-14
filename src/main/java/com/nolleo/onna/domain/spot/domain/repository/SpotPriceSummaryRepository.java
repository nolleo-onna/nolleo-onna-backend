package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;

import java.util.Optional;

/**
 * [도메인 포트] SpotPriceSummary 저장소 인터페이스.
 * 구현체: infrastructure/persistence/SpotPriceSummaryRepositoryImpl
 */
public interface SpotPriceSummaryRepository {

    Optional<SpotPriceSummary> findById(String spotContentId);
}