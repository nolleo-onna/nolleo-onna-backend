package com.nolleo.onna.domain.map.domain.repository;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MapPlaceRepository {

    /** 구·카테고리·예산 조건에 맞는 활성 장소 목록 */
    List<MapPlace> findByFilter(String district, PlaceCategory category, Integer maxBudget);

    /** 지정 구(district)의 활성 장소를 (lat, lon) 기준 가까운 순으로 정렬한 목록 */
    List<MapPlace> findNearbyByDistrict(String district, double lat, double lon);

    /** 지정 ID 목록의 장소를 (lat, lon) 기준 가까운 순으로 정렬한 목록 */
    List<MapPlace> findByIdsOrderByDistance(List<Long> ids, double lat, double lon);

    /** 지정 ID 목록의 장소를 distance 순 정렬하면서 각 장소까지의 거리(m)를 id → distance 맵으로 반환 */
    Map<Long, Integer> findDistancesFromPoint(List<Long> ids, double lat, double lon);

    /** 구·카테고리·예산 조건에 맞는 활성 장소 페이징 목록 */
    Page<MapPlace> findByFilterPaged(String district, PlaceCategory category, Integer maxBudget, Pageable pageable);

    /** id로 장소 단건 조회 */
    Optional<MapPlace> findById(Long id);

    /** id 목록으로 장소 일괄 조회 */
    Map<Long, MapPlace> findAllByIds(Set<Long> ids);

    /** placeType + originalId 로 장소 단건 조회 */
    Optional<MapPlace> findByPlaceTypeAndOriginalId(PlaceType placeType, String originalId);

    /** 장소 저장 (placeType + originalId 기준 upsert) */
    MapPlace save(MapPlace mapPlace);

    /** 구별 활성 장소 수 — category·maxBudget 필터 선택 적용. 반환: district → count */
    Map<String, Long> countByDistrict(PlaceCategory category, Integer maxBudget);

    /** 리뷰 등록 시 avg_rating·review_count 증분 업데이트 */
    void incrementRating(Long mapPlaceId, int rating);

    /** 리뷰 수정 시 avg_rating만 재계산 (review_count 변동 없음) */
    void updateAvgRating(Long mapPlaceId, int oldRating, int newRating);
}
