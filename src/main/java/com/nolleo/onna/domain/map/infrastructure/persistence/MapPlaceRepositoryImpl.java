package com.nolleo.onna.domain.map.infrastructure.persistence;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MapPlaceRepositoryImpl implements MapPlaceRepository {

    private final MapPlaceJpaRepository jpaRepository;

    @Override
    public List<MapPlace> findByFilter(String district, PlaceCategory category, Integer maxBudget) {
        return jpaRepository.findByFilter(district, category, maxBudget).stream()
                .map(MapPlaceEntity::toDomain)
                .toList();
    }

    @Override
    public Page<MapPlace> findByFilterPaged(String district, PlaceCategory category, Integer maxBudget, Pageable pageable) {
        return jpaRepository.findByFilterPaged(district, category, maxBudget, pageable)
                .map(MapPlaceEntity::toDomain);
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
}