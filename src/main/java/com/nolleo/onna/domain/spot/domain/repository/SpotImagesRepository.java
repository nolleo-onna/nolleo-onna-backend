package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.entity.SpotImages;

import java.util.List;
import java.util.Optional;

public interface SpotImagesRepository {

    Optional<SpotImages> findById(Long id);

    List<SpotImages> findByContentId(String contentId);
}