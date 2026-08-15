package com.nolleo.onna.domain.favorite.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.favorite.domain.exception.FavoriteErrorCode;
import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import com.nolleo.onna.domain.favorite.domain.model.FavoritePeriodType;
import com.nolleo.onna.domain.favorite.domain.repository.FavoriteRepository;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteItemResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatsResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatusResponse;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
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

        if (favorites.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> mapPlaceIds = favorites.stream()
                .map(Favorite::mapPlaceId)
                .collect(Collectors.toSet());

        Map<Long, MapPlace> mapPlaceById = mapPlaceRepository.findAllByIds(mapPlaceIds);

        return favorites.map(favorite -> {
            MapPlace place = mapPlaceById.get(favorite.mapPlaceId());
            if (place == null) {
                throw new BusinessException(FavoriteErrorCode.MAP_PLACE_NOT_FOUND);
            }
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

    @Transactional(readOnly = true)
    public FavoriteStatsResponse getStats(Long userId) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(kst);

        OffsetDateTime todayStart = now.toLocalDate().atStartOfDay(kst).toOffsetDateTime();
        long todayCount = favoriteRepository.countByUserIdBetween(userId, todayStart, todayStart.plusDays(1));
        if (todayCount > 0) {
            return FavoriteStatsResponse.of(FavoritePeriodType.TODAY, todayCount);
        }

        OffsetDateTime weekStart = now.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(kst).toOffsetDateTime();
        long weekCount = favoriteRepository.countByUserIdBetween(userId, weekStart, weekStart.plusWeeks(1));
        if (weekCount > 0) {
            return FavoriteStatsResponse.of(FavoritePeriodType.WEEK, weekCount);
        }

        OffsetDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay(kst).toOffsetDateTime();
        long monthCount = favoriteRepository.countByUserIdBetween(userId, monthStart, monthStart.plusMonths(1));
        return FavoriteStatsResponse.of(FavoritePeriodType.MONTH, monthCount);
    }
}
