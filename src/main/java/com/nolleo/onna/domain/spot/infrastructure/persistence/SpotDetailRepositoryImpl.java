package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.SpotDetail;
import com.nolleo.onna.domain.spot.domain.repository.SpotDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** [인프라 어댑터] SpotDetailsRepository 도메인 포트의 JPA 구현체. */
@Repository
@RequiredArgsConstructor
public class SpotDetailRepositoryImpl implements SpotDetailsRepository {

    private final SpotDetailJpaRepository jpaRepository;

    @Override
    public Optional<SpotDetail> findById(String contentId) {
        return jpaRepository.findById(contentId).map(SpotDetailEntity::toDomain);
    }
}