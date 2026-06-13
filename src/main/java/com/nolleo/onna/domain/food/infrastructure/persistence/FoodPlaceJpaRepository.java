package com.nolleo.onna.domain.food.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * [Spring Data JPA] FoodPlaceEntity 저장소.
 * 도메인 레이어에 노출되지 않음 — FoodPlaceRepositoryImpl 을 통해서만 사용.
 */
public interface FoodPlaceJpaRepository extends JpaRepository<FoodPlaceEntity, Long> {

    /**
     * 메뉴 포함 단건 조회 — fetch join 으로 N+1 방지.
     */
    @Query("SELECT f FROM FoodPlaceEntity f LEFT JOIN FETCH f.menus WHERE f.id = :id")
    Optional<FoodPlaceEntity> findByIdWithMenus(@Param("id") Long id);
}
