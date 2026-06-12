package com.nolleo.onna.spot.infrastructure.persistence;

import com.nolleo.onna.spot.domain.entity.SpotImages;
import com.nolleo.onna.spot.domain.repository.SpotImagesRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpotImagesJpaRepository extends JpaRepository<SpotImages, Long>, SpotImagesRepository {

    @Override
    List<SpotImages> findByContentId(String contentId);
}