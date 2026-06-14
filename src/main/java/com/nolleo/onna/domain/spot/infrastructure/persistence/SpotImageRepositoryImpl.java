package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.SpotImage;
import com.nolleo.onna.domain.spot.domain.repository.SpotImagesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** [인프라 어댑터] SpotImagesRepository 도메인 포트의 JPA 구현체. */
@Repository
@RequiredArgsConstructor
public class SpotImageRepositoryImpl implements SpotImagesRepository {

    private final SpotImageJpaRepository jpaRepository;

    @Override
    public Optional<SpotImage> findById(Long id) {
        return jpaRepository.findById(id).map(SpotImageEntity::toDomain);
    }

    @Override
    public List<SpotImage> findByContentId(String contentId) {
        return jpaRepository.findByContentId(contentId).stream()
                .map(SpotImageEntity::toDomain)
                .toList();
    }
}