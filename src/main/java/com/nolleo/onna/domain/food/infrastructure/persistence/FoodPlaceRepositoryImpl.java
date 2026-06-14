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

    /** fetch join으로 menus를 함께 로드 — N+1 없음 */
    @Override
    public Optional<FoodPlace> findById(Long id) {
        return jpaRepository.findByIdWithMenus(id)
                .map(FoodPlaceEntity::toDomain);
    }

    @Override
    public List<FoodPlace> findAllActive() {
        return jpaRepository.findAllActive().stream()
                .map(FoodPlaceEntity::toDomainWithoutMenus)
                .toList();
    }
}
