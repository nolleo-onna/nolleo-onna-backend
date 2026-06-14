package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.model.Spot;

import java.util.List;
import java.util.Optional;

/**
 * [도메인 포트] Spot 저장소 인터페이스.
 * 구현체: infrastructure/persistence/SpotRepositoryImpl
 */
public interface SpotsRepository {

    Optional<Spot> findById(String contentId);

    List<Spot> findAll();

    List<Spot> findAllActive();
}