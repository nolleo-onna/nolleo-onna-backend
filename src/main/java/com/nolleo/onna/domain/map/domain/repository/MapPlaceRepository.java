package com.nolleo.onna.domain.map.domain.repository;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MapPlaceRepository {

    List<MapPlace> findByFilter(String district, PlaceCategory category, Integer maxBudget);

    Page<MapPlace> findByFilterPaged(String district, PlaceCategory category, Integer maxBudget, Pageable pageable);

    Optional<MapPlace> findByPlaceTypeAndOriginalId(PlaceType placeType, String originalId);

    MapPlace save(MapPlace mapPlace);
}