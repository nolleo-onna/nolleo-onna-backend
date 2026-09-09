package com.nolleo.onna.domain.favorite.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.favorite.domain.exception.FavoriteErrorCode;
import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import com.nolleo.onna.domain.favorite.domain.repository.FavoriteRepository;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteToggleResponse;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteCommandService {

    private final FavoriteRepository favoriteRepository;
    private final MapPlaceRepository mapPlaceRepository;

    @Transactional
    public FavoriteToggleResponse toggle(Long userId, Long mapPlaceId) {
        mapPlaceRepository.findById(mapPlaceId)
                .orElseThrow(() -> new BusinessException(FavoriteErrorCode.MAP_PLACE_NOT_FOUND));

        Optional<Favorite> existing = favoriteRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            mapPlaceRepository.decrementFavoriteCount(mapPlaceId);
            return new FavoriteToggleResponse(mapPlaceId, false);
        }

        favoriteRepository.save(new Favorite(null, userId, mapPlaceId, null));
        mapPlaceRepository.incrementFavoriteCount(mapPlaceId);
        return new FavoriteToggleResponse(mapPlaceId, true);
    }
}
