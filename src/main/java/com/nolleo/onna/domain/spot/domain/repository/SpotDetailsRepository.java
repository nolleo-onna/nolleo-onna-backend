package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.entity.SpotDetails;

import java.util.Optional;

public interface SpotDetailsRepository {

    Optional<SpotDetails> findById(String contentId);
}