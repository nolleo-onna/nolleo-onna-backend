package com.nolleo.onna.domain.spot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpotJpaRepository extends JpaRepository<SpotEntity, String> {

    @Query("SELECT s FROM SpotEntity s WHERE s.active = true")
    List<SpotEntity> findAllActive();

    @Query("SELECT s FROM SpotEntity s WHERE s.contentId IN :ids AND s.active = true")
    List<SpotEntity> findActiveByIds(@Param("ids") List<String> ids);

    /**
     * 활성 스팟 중 카테고리 목록에 속하고 기준점에서 radiusM(미터) 안에 있는 스팟을 가까운 순으로 limit개 조회.
     *
     * ORDER BY geog <-> point 는 LIMIT이 붙어야 GiST KNN 인덱스 스캔이 된다 — LIMIT 없이 부르면
     * 카테고리 전체를 정렬해서 전부 전송하므로 반드시 limit을 지정한다.
     * ST_DWithin은 geography 기준이라 radiusM 단위는 미터다. geog가 NULL인 스팟은 반경 조건에서 걸러진다.
     */
    @Query(value = """
            SELECT * FROM sp_spots
            WHERE is_active = true
              AND lcls_systm_1 IN (:codes)
              AND ST_DWithin(geog, ST_MakePoint(:lon, :lat)::geography, :radiusM)
            ORDER BY geog <-> ST_MakePoint(:lon, :lat)::geography
            LIMIT :limit
            """, nativeQuery = true)
    List<SpotEntity> findNearbyByCategories(@Param("codes") List<String> codes,
                                             @Param("lat") double lat,
                                             @Param("lon") double lon,
                                             @Param("radiusM") double radiusM,
                                             @Param("limit") int limit);

    /**
     * 활성 스팟 중 제목이 키워드를 포함하는 스팟을 "정확히 일치 → 기준점에서 가까운 순"으로 limit개 조회.
     *
     * 비교는 양쪽 공백을 제거하고 대소문자 무시로 한다 — 관광공사 제목은 "광안리해수욕장"처럼 붙여 쓰고
     * 사용자는 "광안리 해수욕장"처럼 띄어 말하기 때문이다. compactKeyword는 호출자가 공백을 제거하고
     * LIKE 와일드카드(% _ \)를 이스케이프해서 넘긴다.
     */
    @Query(value = """
            SELECT * FROM sp_spots
            WHERE is_active = true
              AND REPLACE(title, ' ', '') ILIKE CONCAT('%', :compactKeyword, '%')
            ORDER BY (LOWER(REPLACE(title, ' ', '')) = LOWER(:compactKeyword)) DESC,
                     geog <-> ST_MakePoint(:lon, :lat)::geography
            LIMIT :limit
            """, nativeQuery = true)
    List<SpotEntity> findActiveByTitleNear(@Param("compactKeyword") String compactKeyword,
                                           @Param("lat") double lat,
                                           @Param("lon") double lon,
                                           @Param("limit") int limit);

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