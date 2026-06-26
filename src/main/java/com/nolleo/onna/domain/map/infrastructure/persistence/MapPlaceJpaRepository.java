package com.nolleo.onna.domain.map.infrastructure.persistence;

import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MapPlaceJpaRepository extends JpaRepository<MapPlaceEntity, Long> {

    @Query("SELECT e FROM MapPlaceEntity e WHERE e.active = true " +
           "AND (:district IS NULL OR e.district = :district) " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:maxBudget IS NULL OR e.free = true OR e.minPrice IS NULL OR e.minPrice <= :maxBudget)")
    Page<MapPlaceEntity> findByFilterPaged(
        @Param("district") String district,
        @Param("category") PlaceCategory category,
        @Param("maxBudget") Integer maxBudget,
        Pageable pageable
    );

    Optional<MapPlaceEntity> findByPlaceTypeAndOriginalId(PlaceType placeType, String originalId);
}
