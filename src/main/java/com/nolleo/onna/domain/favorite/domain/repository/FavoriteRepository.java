package com.nolleo.onna.domain.favorite.domain.repository;

import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface FavoriteRepository {

    Favorite save(Favorite favorite);

    void delete(Favorite favorite);

    Optional<Favorite> findByUserIdAndMapPlaceId(Long userId, Long mapPlaceId);

    Page<Favorite> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndMapPlaceId(Long userId, Long mapPlaceId);

    long countByUserIdBetween(Long userId, OffsetDateTime start, OffsetDateTime end);
}
