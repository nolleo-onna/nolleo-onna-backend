package com.nolleo.onna.spot.domain.repository;

import com.nolleo.onna.spot.domain.entity.SpotImages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpotImagesRepository extends JpaRepository<SpotImages, Long> {

    List<SpotImages> findByContentId(String contentId);
}