package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.model.SpotImage;

import java.util.List;
import java.util.Optional;

/**
 * [도메인 포트] SpotImage 저장소 인터페이스.
 * 구현체: infrastructure/persistence/SpotImageRepositoryImpl
 */
public interface SpotImagesRepository {

    Optional<SpotImage> findById(Long id);

    List<SpotImage> findByContentId(String contentId);
}