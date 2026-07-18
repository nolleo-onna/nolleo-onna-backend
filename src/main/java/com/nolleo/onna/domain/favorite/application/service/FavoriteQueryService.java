package com.nolleo.onna.domain.favorite.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.favorite.domain.exception.FavoriteErrorCode;
import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import com.nolleo.onna.domain.favorite.domain.repository.FavoriteRepository;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteItemResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatusResponse;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteQueryService {

    private final FavoriteRepository favoriteRepository;
    private final MapPlaceRepository mapPlaceRepository;

    @Transactional(readOnly = true)
    public Page<FavoriteItemResponse> getFavorites(Long userId, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository.findAllByUserId(userId, pageable);

        Set<Long> mapPlaceIds = favorites.stream()
                .map(Favorite::mapPlaceId)
                .collect(Collectors.toSet());

        Map<Long, MapPlace> mapPlaceById = mapPlaceRepository.findAllByIds(mapPlaceIds);

        return favorites.map(favorite -> {
            MapPlace place = mapPlaceById.get(favorite.mapPlaceId());
            return FavoriteItemResponse.of(favorite, place);
        });
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse getStatus(Long userId, Long mapPlaceId) {
        mapPlaceRepository.findById(mapPlaceId)
                .orElseThrow(() -> new BusinessException(FavoriteErrorCode.MAP_PLACE_NOT_FOUND));

        boolean favorited = favoriteRepository.existsByUserIdAndMapPlaceId(userId, mapPlaceId);
        return new FavoriteStatusResponse(mapPlaceId, favorited);
    }
}
