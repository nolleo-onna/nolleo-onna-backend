package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.model.SpotDetail;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * [도메인 포트] SpotDetail 저장소 인터페이스.
 * 구현체: infrastructure/persistence/SpotDetailRepositoryImpl
 */
public interface SpotDetailsRepository {

    Optional<SpotDetail> findById(String contentId);

    Map<String, SpotDetail> findAllByIds(List<String> contentIds);
}