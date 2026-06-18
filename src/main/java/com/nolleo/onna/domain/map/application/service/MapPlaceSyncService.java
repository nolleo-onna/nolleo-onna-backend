package com.nolleo.onna.domain.map.application.service;

import com.nolleo.onna.domain.food.domain.model.FoodPlace;
import com.nolleo.onna.domain.food.domain.model.FoodPlaceMenu;
import com.nolleo.onna.domain.food.domain.repository.FoodPlaceRepository;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.SpotDetail;
import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.repository.SpotDetailsRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotPriceSummaryRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class MapPlaceSyncService {

    private final MapPlaceRepository mapPlaceRepository;
    private final FoodPlaceRepository foodPlaceRepository;
    private final SpotsRepository spotsRepository;
    private final SpotDetailsRepository spotDetailsRepository;
    private final SpotPriceSummaryRepository spotPriceSummaryRepository;

    @Transactional
    public void syncAll() {
        syncAllFood();
        syncAllSpots();
    }

    @Transactional
    public void syncAllFood() {
        foodPlaceRepository.findAllActiveWithMenus().forEach(this::syncFood);
    }

    @Transactional
    public void syncAllSpots() {
        spotsRepository.findAllActive().forEach(spot -> {
            SpotDetail detail = spotDetailsRepository.findById(spot.getContentId()).orElse(null);
            SpotPriceSummary priceSummary = spotPriceSummaryRepository.findById(spot.getContentId()).orElse(null);
            syncSpot(spot, detail, priceSummary);
        });
    }

    public void syncFood(FoodPlace food) {
        Integer minPrice = food.getMenus().stream()
                .map(FoodPlaceMenu::getMoney)
                .filter(m -> m != null && m.price() != null)
                .map(m -> m.price())
                .min(Comparator.naturalOrder())
                .orElse(null);

        MapPlace mapPlace = new MapPlace(
                null,
                PlaceType.FOOD,
                String.valueOf(food.getId()),
                food.getName(),
                food.getSourceRegion(),
                PlaceCategory.FD,
                food.getGeoCoordinate() != null ? food.getGeoCoordinate().longitude() : null,
                food.getGeoCoordinate() != null ? food.getGeoCoordinate().latitude() : null,
                null,
                minPrice,
                minPrice != null && minPrice == 0,
                food.isActive()
        );
        mapPlaceRepository.save(mapPlace);
    }

    public void syncSpot(Spot spot, SpotDetail detail, SpotPriceSummary priceSummary) {
        PlaceCategory category = PlaceCategory.fromSpotLclsSystm1(spot.getLclsSystm1());
        if (category == null) return;

        Integer minPrice = priceSummary != null ? priceSummary.getMinPrice() : null;
        String district = detail != null ? extractDistrict(detail.getAddr1()) : null;

        MapPlace mapPlace = new MapPlace(
                null,
                PlaceType.SPOT,
                spot.getContentId(),
                spot.getTitle(),
                district,
                category,
                spot.getGeoCoordinate() != null ? spot.getGeoCoordinate().longitude() : null,
                spot.getGeoCoordinate() != null ? spot.getGeoCoordinate().latitude() : null,
                spot.getFirstImage(),
                minPrice,
                minPrice != null && minPrice == 0,
                spot.isActive()
        );
        mapPlaceRepository.save(mapPlace);
    }

    static String extractDistrict(String address) {
        if (address == null || address.isBlank()) return null;
        String[] tokens = address.trim().split("\\s+");
        return tokens.length >= 2 ? tokens[1] : null;
    }
}
