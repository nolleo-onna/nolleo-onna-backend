package com.nolleo.onna.domain.map.application.service;

import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import com.nolleo.onna.domain.map.presentation.dto.response.DistrictCountResponse;
import com.nolleo.onna.domain.map.presentation.dto.response.MapMarkerResponse;
import com.nolleo.onna.domain.map.presentation.dto.response.MapPlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapQueryService {

    private final MapPlaceRepository mapPlaceRepository;

    public List<MapMarkerResponse> getMapMarkers(String district, PlaceCategory category, Integer maxBudget) {
        return mapPlaceRepository.findByFilter(district, category, maxBudget).stream()
                .map(MapMarkerResponse::fromMapPlace)
                .toList();
    }

    public Page<MapPlaceResponse> getMapPlaces(String district, PlaceCategory category, Integer maxBudget, Pageable pageable) {
        return mapPlaceRepository.findByFilterPaged(district, category, maxBudget, pageable)
                .map(MapPlaceResponse::from);
    }

    public List<DistrictCountResponse> getDistrictCounts(PlaceCategory category, Integer maxBudget) {
        return mapPlaceRepository.countByDistrict(category, maxBudget).entrySet().stream()
                .map(e -> new DistrictCountResponse(e.getKey(), e.getValue()))
                .toList();
    }
}
