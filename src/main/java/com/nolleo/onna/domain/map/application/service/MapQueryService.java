package com.nolleo.onna.domain.map.application.service;

import com.nolleo.onna.domain.food.domain.repository.FoodPlaceRepository;
import com.nolleo.onna.domain.map.presentation.dto.response.MapMarkerResponse;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapQueryService {

    private final SpotsRepository spotsRepository;
    private final FoodPlaceRepository foodPlaceRepository;

    public List<MapMarkerResponse> getMapMarkers() {
        List<MapMarkerResponse> spotMarkers = spotsRepository.findAllActive().stream()
                .map(MapMarkerResponse::fromSpot)
                .toList();

        List<MapMarkerResponse> foodMarkers = foodPlaceRepository.findAllActive().stream()
                .map(MapMarkerResponse::fromFood)
                .toList();

        return Stream.concat(spotMarkers.stream(), foodMarkers.stream()).toList();
    }
}