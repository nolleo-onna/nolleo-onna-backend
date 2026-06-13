package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.entity.Spots;

import java.util.List;
import java.util.Optional;

public interface SpotsRepository {

    Optional<Spots> findById(String contentId);

    List<Spots> findAll();

    List<Spots> findAllActive();
}