package com.nolleo.onna.spot.domain.repository;

import com.nolleo.onna.spot.domain.entity.SpotDetails;

import java.util.Optional;

public interface SpotDetailsRepository {

    Optional<SpotDetails> findById(String contentId);
}