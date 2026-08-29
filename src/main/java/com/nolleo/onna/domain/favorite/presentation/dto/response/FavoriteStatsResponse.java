package com.nolleo.onna.domain.favorite.presentation.dto.response;

import com.nolleo.onna.domain.favorite.domain.model.FavoritePeriodType;

public record FavoriteStatsResponse(
        FavoritePeriodType period,
        long count,
        String message
) {

    public static FavoriteStatsResponse of(FavoritePeriodType period, long count) {
        String message = switch (period) {
            case TODAY -> "오늘 " + count + "개 찜했어요!";
            case WEEK -> "이번 주 " + count + "개 찜했어요!";
            case MONTH -> count == 0 ? "이번 달에 찜한 장소가 없어요" : "이번 달 " + count + "개 찜했어요!";
        };
        return new FavoriteStatsResponse(period, count, message);
    }
}
