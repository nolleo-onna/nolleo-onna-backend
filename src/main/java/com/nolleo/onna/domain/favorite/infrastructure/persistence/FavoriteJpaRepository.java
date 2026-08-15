package com.nolleo.onna.domain.favorite.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface FavoriteJpaRepository extends JpaRepository<FavoriteEntity, Long> {

    Optional<FavoriteEntity> findByUserIdAndMapPlaceId(Long userId, Long mapPlaceId);

    Page<FavoriteEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndMapPlaceId(Long userId, Long mapPlaceId);

    @Query("SELECT COUNT(f) FROM FavoriteEntity f WHERE f.userId = :userId AND f.createdAt >= :start AND f.createdAt < :end")
    long countByUserIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
