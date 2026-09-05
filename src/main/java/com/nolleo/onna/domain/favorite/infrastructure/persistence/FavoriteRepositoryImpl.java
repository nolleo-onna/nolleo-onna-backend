package com.nolleo.onna.domain.favorite.infrastructure.persistence;

import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import com.nolleo.onna.domain.favorite.domain.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FavoriteRepositoryImpl implements FavoriteRepository {

    private final FavoriteJpaRepository jpaRepository;

    @Override
    public Favorite save(Favorite favorite) {
        return jpaRepository.save(FavoriteEntity.from(favorite)).toDomain();
    }

    @Override
    public void delete(Favorite favorite) {
        jpaRepository.deleteById(favorite.id());
    }

    @Override
    public Optional<Favorite> findByUserIdAndMapPlaceId(Long userId, Long mapPlaceId) {
        return jpaRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId)
                .map(FavoriteEntity::toDomain);
    }

    @Override
    public Page<Favorite> findAllByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(FavoriteEntity::toDomain);
    }

    @Override
    public boolean existsByUserIdAndMapPlaceId(Long userId, Long mapPlaceId) {
        return jpaRepository.existsByUserIdAndMapPlaceId(userId, mapPlaceId);
    }

    @Override
    public long countByUserIdBetween(Long userId, OffsetDateTime start, OffsetDateTime end) {
        return jpaRepository.countByUserIdAndCreatedAtBetween(userId, start, end);
    }
}
