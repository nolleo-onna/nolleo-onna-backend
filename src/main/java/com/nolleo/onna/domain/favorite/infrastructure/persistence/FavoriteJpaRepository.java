package com.nolleo.onna.domain.favorite.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteJpaRepository extends JpaRepository<FavoriteEntity, Long> {

    Optional<FavoriteEntity> findByUserIdAndMapPlaceId(Long userId, Long mapPlaceId);

    Page<FavoriteEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndMapPlaceId(Long userId, Long mapPlaceId);
}
