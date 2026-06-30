package com.nolleo.onna.domain.map.infrastructure.persistence;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MapPlaceRepositoryImpl implements MapPlaceRepository {

    private final MapPlaceJpaRepository jpaRepository;

    @Override
    public List<MapPlace> findByFilter(String district, PlaceCategory category, Integer maxBudget) {
        return jpaRepository.findByFilterPaged(district, category, maxBudget, Pageable.unpaged())
                .map(MapPlaceEntity::toDomain)
                .getContent();
    }

    @Override
    public Page<MapPlace> findByFilterPaged(String district, PlaceCategory category, Integer maxBudget, Pageable pageable) {
        return jpaRepository.findByFilterPaged(district, category, maxBudget, pageable)
                .map(MapPlaceEntity::toDomain);
    }

    @Override
    public List<MapPlace> findNearbyByDistrict(String district, double lat, double lon) {
        return jpaRepository.findNearbyByDistrict(district, lat, lon).stream()
                .map(MapPlaceEntity::toDomain)
                .toList();
    }

    @Override
    public List<MapPlace> findByIdsOrderByDistance(List<Long> ids, double lat, double lon) {
        return jpaRepository.findByIdsOrderByDistance(ids, lat, lon).stream()
                .map(MapPlaceEntity::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Integer> findDistancesFromPoint(List<Long> ids, double lat, double lon) {
        List<Object[]> rows = jpaRepository.findByIdsWithDistanceFromPoint(ids, lat, lon);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            int distance = row[1] != null ? ((Number) row[1]).intValue() : 0;
            result.put(id, distance);
        }
        return result;
    }

    @Override
    public Optional<MapPlace> findById(Long id) {
        return jpaRepository.findById(id).map(MapPlaceEntity::toDomain);
    }

    @Override
    public Map<Long, MapPlace> findAllByIds(Set<Long> ids) {
        return jpaRepository.findAllById(ids).stream()
                .map(MapPlaceEntity::toDomain)
                .collect(Collectors.toMap(MapPlace::getId, p -> p));
    }

    @Override
    public Optional<MapPlace> findByPlaceTypeAndOriginalId(PlaceType placeType, String originalId) {
        return jpaRepository.findByPlaceTypeAndOriginalId(placeType, originalId)
                .map(MapPlaceEntity::toDomain);
    }

    @Override
    public MapPlace save(MapPlace mapPlace) {
        return jpaRepository.findByPlaceTypeAndOriginalId(mapPlace.getPlaceType(), mapPlace.getOriginalId())
                .map(entity -> {
                    entity.updateFrom(mapPlace);
                    return jpaRepository.save(entity).toDomain();
                })
                .orElseGet(() -> jpaRepository.save(MapPlaceEntity.from(mapPlace)).toDomain());
    }

    @Override
    public Map<String, Long> countByDistrict(PlaceCategory category, Integer maxBudget) {
        return jpaRepository.countGroupByDistrict(category, maxBudget).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public void incrementRating(Long mapPlaceId, int rating) {
        jpaRepository.incrementRating(mapPlaceId, rating);
    }
}