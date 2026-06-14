package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** [인프라 어댑터] SpotsRepository 도메인 포트의 JPA 구현체. */
@Repository
@RequiredArgsConstructor
public class SpotRepositoryImpl implements SpotsRepository {

    private final SpotJpaRepository jpaRepository;

    @Override
    public Optional<Spot> findById(String contentId) {
        return jpaRepository.findById(contentId).map(SpotEntity::toDomain);
    }

    @Override
    public List<Spot> findAll() {
        return jpaRepository.findAll().stream().map(SpotEntity::toDomain).toList();
    }

    @Override
    public List<Spot> findAllActive() {
        return jpaRepository.findAllActive().stream().map(SpotEntity::toDomain).toList();
    }
}