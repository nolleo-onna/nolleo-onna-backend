package com.nolleo.onna.domain.map.infrastructure.persistence;

import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MapPlaceJpaRepository extends JpaRepository<MapPlaceEntity, Long> {

    @Query(value = "SELECT e FROM MapPlaceEntity e WHERE e.active = true " +
                  "AND (:district IS NULL OR e.district = :district) " +
                  "AND (:category IS NULL OR e.category = :category) " +
                  "AND (:maxBudget IS NULL OR e.free = true OR e.minPrice IS NULL OR e.minPrice <= :maxBudget) " +
                  "AND (:keyword IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                  "ORDER BY CASE WHEN e.category = 'VE' THEN 0 ELSE 1 END",
           countQuery = "SELECT COUNT(e) FROM MapPlaceEntity e WHERE e.active = true " +
                       "AND (:district IS NULL OR e.district = :district) " +
                       "AND (:category IS NULL OR e.category = :category) " +
                       "AND (:maxBudget IS NULL OR e.free = true OR e.minPrice IS NULL OR e.minPrice <= :maxBudget) " +
                       "AND (:keyword IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<MapPlaceEntity> findByFilterPaged(
        @Param("district") String district,
        @Param("category") PlaceCategory category,
        @Param("maxBudget") Integer maxBudget,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    Optional<MapPlaceEntity> findByPlaceTypeAndOriginalId(PlaceType placeType, String originalId);

    @Query("SELECT e.district, COUNT(e) FROM MapPlaceEntity e WHERE e.active = true " +
           "AND e.district IS NOT NULL " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:maxBudget IS NULL OR e.free = true OR e.minPrice IS NULL OR e.minPrice <= :maxBudget) " +
           "GROUP BY e.district ORDER BY e.district")
    List<Object[]> countGroupByDistrict(
        @Param("category") PlaceCategory category,
        @Param("maxBudget") Integer maxBudget
    );

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

    /** 리뷰 등록 시 avg_rating·review_count 증분 업데이트 */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE mp_map_places
            SET review_count = review_count + 1,
                avg_rating   = ROUND((avg_rating * review_count + CAST(:rating AS NUMERIC)) / (review_count + 1), 2)
            WHERE id = :id
            """, nativeQuery = true)
    void incrementRating(@Param("id") Long id, @Param("rating") int rating);

    /** 리뷰 수정 시 avg_rating만 재계산 (review_count 변동 없음) */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE mp_map_places
            SET avg_rating = CASE
                WHEN review_count = 0 THEN 0
                ELSE ROUND((avg_rating * review_count - CAST(:oldRating AS NUMERIC) + CAST(:newRating AS NUMERIC)) / review_count, 2)
            END
            WHERE id = :id
            """, nativeQuery = true)
    void updateAvgRating(@Param("id") Long id, @Param("oldRating") int oldRating, @Param("newRating") int newRating);

    /** 찜 추가 시 favorite_count 원자적 증가 */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE mp_map_places
            SET favorite_count = favorite_count + 1
            WHERE id = :id
            """, nativeQuery = true)
    void incrementFavoriteCount(@Param("id") Long id);

    /** 찜 취소 시 favorite_count 원자적 감소 — 0 미만 방지 */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE mp_map_places
            SET favorite_count = favorite_count - 1
            WHERE id = :id AND favorite_count > 0
            """, nativeQuery = true)
    void decrementFavoriteCount(@Param("id") Long id);

    /** 장소 목록을 distance 순으로 정렬하면서 각 장소까지의 거리(m)도 함께 반환 — [id, distanceMeters] */
    @Query(value = """
            SELECT id, ST_Distance(geog, ST_MakePoint(:lon, :lat)::geography) AS distance_m
            FROM mp_map_places
            WHERE id IN (:ids)
            ORDER BY distance_m
            """, nativeQuery = true)
    List<Object[]> findByIdsWithDistanceFromPoint(
            @Param("ids") List<Long> ids,
            @Param("lat") double lat,
            @Param("lon") double lon
    );
}
