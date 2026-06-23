package com.nolleo.onna.domain.map.infrastructure.persistence;

import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query(value = """
            SELECT * FROM mp_map_places
            WHERE is_active = true
            AND (:district IS NULL OR district = :district)
            ORDER BY geog <-> ST_MakePoint(:lon, :lat)::geography
            """, nativeQuery = true)
    List<MapPlaceEntity> findNearbyByDistrict(
            @Param("district") String district,
            @Param("lat") double lat,
            @Param("lon") double lon
    );

    @Query(value = """
            SELECT * FROM mp_map_places
            WHERE id IN (:ids)
            ORDER BY geog <-> ST_MakePoint(:lon, :lat)::geography
            """, nativeQuery = true)
    List<MapPlaceEntity> findByIdsOrderByDistance(
            @Param("ids") List<Long> ids,
            @Param("lat") double lat,
            @Param("lon") double lon
    );

    @Query(value = """
            SELECT ST_Distance(a.geog, b.geog)
            FROM mp_map_places a, mp_map_places b
            WHERE a.id = :id1 AND b.id = :id2
            """, nativeQuery = true)
    Double distanceMetersBetween(@Param("id1") Long id1, @Param("id2") Long id2);

    @Query(value = """
            SELECT ST_Distance(ST_MakePoint(:lon, :lat)::geography, geog)
            FROM mp_map_places
            WHERE id = :id
            """, nativeQuery = true)
    Double distanceMetersFromPoint(@Param("lat") double lat, @Param("lon") double lon, @Param("id") Long id);
}
