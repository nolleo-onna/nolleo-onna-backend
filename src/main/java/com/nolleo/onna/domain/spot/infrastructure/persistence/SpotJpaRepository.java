package com.nolleo.onna.domain.spot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpotJpaRepository extends JpaRepository<SpotEntity, String> {

    @Query("SELECT s FROM SpotEntity s WHERE s.active = true")
    List<SpotEntity> findAllActive();

    @Query(value = """
            SELECT * FROM sp_spots
            WHERE is_active = true
            AND (:lclsSystm1 IS NULL OR lcls_systm_1 = :lclsSystm1)
            ORDER BY geog <-> ST_MakePoint(:lon, :lat)::geography
            """, nativeQuery = true)
    List<SpotEntity> findNearbyByCategory(@Param("lclsSystm1") String lclsSystm1,
                                           @Param("lat") double lat,
                                           @Param("lon") double lon);

    @Query(value = """
            SELECT * FROM sp_spots WHERE content_id IN (:ids)
            ORDER BY geog <-> ST_MakePoint(:lon, :lat)::geography
            """, nativeQuery = true)
    List<SpotEntity> findByIdsOrderByDistance(@Param("ids") List<String> ids,
                                               @Param("lat") double lat,
                                               @Param("lon") double lon);

    @Query(value = """
            SELECT content_id, ST_Distance(geog, ST_MakePoint(:lon, :lat)::geography) AS distance_m
            FROM sp_spots WHERE content_id IN (:ids)
            ORDER BY distance_m
            """, nativeQuery = true)
    List<Object[]> findByIdsWithDistanceFromPoint(@Param("ids") List<String> ids,
                                                   @Param("lat") double lat,
                                                   @Param("lon") double lon);
}