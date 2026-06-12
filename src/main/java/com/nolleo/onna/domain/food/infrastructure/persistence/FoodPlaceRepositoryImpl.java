package com.nolleo.onna.domain.food.infrastructure.persistence;

import com.nolleo.onna.domain.food.domain.model.FoodPlace;
import com.nolleo.onna.domain.food.domain.repository.FoodPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** [인프라 어댑터] FoodPlaceRepository 도메인 포트의 JPA 구현체. */
@Repository
@RequiredArgsConstructor
public class FoodPlaceRepositoryImpl implements FoodPlaceRepository {

    private final FoodPlaceJpaRepository jpaRepository;

    @Override
    public Optional<FoodPlace> findById(Long id) {
        return jpaRepository.findByIdWithMenus(id)
                .map(FoodPlaceEntity::toDomain);
    }

    @Override
    public List<FoodPlace> findAllActive() {
        return jpaRepository.findAllByActiveTrue().stream()
                .map(FoodPlaceEntity::toDomain)
                .toList();
    }

    @Override
    public List<FoodPlace> findAllByNormalizedCategory(String normalizedCategory) {
        return jpaRepository.findAllByNormalizedCategoryAndActiveTrue(normalizedCategory).stream()
                .map(FoodPlaceEntity::toDomain)
                .toList();
    }

    @Override
    public List<FoodPlace> findAllCourseFoodCandidates() {
        return jpaRepository.findAllByCourseFoodCandidateTrueAndActiveTrue().stream()
                .map(FoodPlaceEntity::toDomain)
                .toList();
    }

    @Override
    public List<FoodPlace> findAllBySourceRegion(String sourceRegion) {
        return jpaRepository.findAllBySourceRegionAndActiveTrue(sourceRegion).stream()
                .map(FoodPlaceEntity::toDomain)
                .toList();
    }
}
