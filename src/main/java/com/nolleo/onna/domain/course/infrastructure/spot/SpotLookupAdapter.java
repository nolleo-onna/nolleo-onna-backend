package com.nolleo.onna.domain.course.infrastructure.spot;

import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;
import com.nolleo.onna.domain.spot.domain.model.vo.SpotCategory;
import com.nolleo.onna.domain.spot.domain.repository.SpotPriceSummaryRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SpotLookupPort의 어댑터.
 * Spot 컨텍스트의 도메인 모델(Spot/SpotCategory/GeoCoordinate)과 리포지토리를
 * Course 컨텍스트가 이해하는 SpotCandidate로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class SpotLookupAdapter implements SpotLookupPort {

    private final SpotsRepository spotsRepository;
    private final SpotPriceSummaryRepository spotPriceSummaryRepository;

    @Override
    public List<SpotCandidate> findNearbyByCategory(String categoryCode, double lat, double lon) {
        return spotsRepository.findNearbyByCategory(categoryCode, lat, lon).stream()
                .map(SpotLookupAdapter::toCandidate)
                .toList();
    }

    @Override
    public Map<String, SpotCandidate> findByIds(List<String> contentIds) {
        if (contentIds.isEmpty()) return Map.of();
        return spotsRepository.findByIds(contentIds).stream()
                .map(SpotLookupAdapter::toCandidate)
                .collect(Collectors.toMap(SpotCandidate::contentId, Function.identity()));
    }

    @Override
    public Map<String, Integer> findFoodPrices(List<String> foodContentIds) {
        if (foodContentIds.isEmpty()) return Map.of();
        Map<String, SpotPriceSummary> prices = spotPriceSummaryRepository.findAllByIds(foodContentIds);

        Map<String, Integer> result = new LinkedHashMap<>();
        prices.forEach((contentId, price) -> {
            Integer cost = price.getMinPrice() != null ? price.getMinPrice() : price.getRepresentativePrice();
            if (cost != null) result.put(contentId, cost);
        });
        return result;
    }

    private static SpotCandidate toCandidate(Spot spot) {
        SpotCategory category = spot.getCategory();
        GeoCoordinate geo = spot.getGeoCoordinate();
        return new SpotCandidate(
                spot.getContentId(),
                spot.getTitle(),
                spot.getFirstImage(),
                category != null ? category.name() : null,
                category != null ? category.getLabel() : null,
                geo != null ? geo.longitude() : null,
                geo != null ? geo.latitude() : null
        );
    }
}
