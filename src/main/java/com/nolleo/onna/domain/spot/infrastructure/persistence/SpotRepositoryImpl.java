package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** [인프라 어댑터] SpotsRepository 도메인 포트의 JPA 구현체. */
@Repository
@RequiredArgsConstructor
public class SpotRepositoryImpl implements SpotsRepository {

    private final SpotJpaRepository jpaRepository;

    @Override
    public Optional<Spot> findById(String contentId) {
        return jpaRepository.findById(contentId).map(SpotEntity::toDomain);
    }

    @Override
    public List<Spot> findAll() {
        return jpaRepository.findAll().stream().map(SpotEntity::toDomain).toList();
    }

    @Override
    public List<Spot> findAllActive() {
        return jpaRepository.findAllActive().stream().map(SpotEntity::toDomain).toList();
    }

    @Override
    public List<Spot> findByIds(List<String> contentIds) {
        if (contentIds.isEmpty()) return List.of();
        return jpaRepository.findAllById(contentIds).stream().map(SpotEntity::toDomain).toList();
    }

    @Override
    public List<Spot> findNearbyByCategory(String lclsSystm1, double lat, double lon) {
        return jpaRepository.findNearbyByCategory(lclsSystm1, lat, lon).stream()
                .map(SpotEntity::toDomain)
                .toList();
    }

    @Override
    public List<Spot> findByIdsOrderByDistance(List<String> contentIds, double lat, double lon) {
        if (contentIds.isEmpty()) return List.of();
        return jpaRepository.findByIdsOrderByDistance(contentIds, lat, lon).stream()
                .map(SpotEntity::toDomain)
                .toList();
    }

    @Override
    public Map<String, Integer> findDistancesFromPoint(List<String> contentIds, double lat, double lon) {
        if (contentIds.isEmpty()) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Object[] row : jpaRepository.findByIdsWithDistanceFromPoint(contentIds, lat, lon)) {
            String contentId = (String) row[0];
            Number distanceM = (Number) row[1];
            result.put(contentId, distanceM.intValue());
        }
        return result;
    }
}