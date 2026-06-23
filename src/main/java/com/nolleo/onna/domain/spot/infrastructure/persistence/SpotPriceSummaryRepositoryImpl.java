package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.repository.SpotPriceSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** [인프라 어댑터] SpotPriceSummaryRepository 도메인 포트의 JPA 구현체. */
@Repository
@RequiredArgsConstructor
public class SpotPriceSummaryRepositoryImpl implements SpotPriceSummaryRepository {

    private final SpotPriceSummaryJpaRepository jpaRepository;

    @Override
    public Optional<SpotPriceSummary> findById(String spotContentId) {
        return jpaRepository.findById(spotContentId).map(SpotPriceSummaryEntity::toDomain);
    }

    @Override
    public Map<String, SpotPriceSummary> findAllByIds(List<String> contentIds) {
        return jpaRepository.findAllById(contentIds).stream()
                .map(SpotPriceSummaryEntity::toDomain)
                .collect(Collectors.toMap(SpotPriceSummary::getSpotContentId, p -> p));
    }
}
