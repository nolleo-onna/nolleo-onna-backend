package com.nolleo.onna.domain.favorite.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.favorite.domain.exception.FavoriteErrorCode;
import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import com.nolleo.onna.domain.favorite.domain.repository.FavoriteRepository;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteItemResponse;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteStatusResponse;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FavoriteQueryServiceTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock MapPlaceRepository mapPlaceRepository;

    @InjectMocks FavoriteQueryService favoriteQueryService;

    @Test
    @DisplayName("사용자별 찜 목록이 MapPlace 정보와 함께 정상적으로 조회된다")
    void getFavorites_returnsPageWithMapPlaceInfo() {
        // given
        Long userId = 1L;
        Long mapPlaceId = 10L;
        Pageable pageable = PageRequest.of(0, 20);

        Favorite favorite = new Favorite(1L, userId, mapPlaceId, OffsetDateTime.now());
        MapPlace mapPlace = new MapPlace(
                mapPlaceId, PlaceType.SPOT, "original-1", "테스트 장소",
                "마포구", PlaceCategory.NA, BigDecimal.valueOf(126.9), BigDecimal.valueOf(37.5),
                null, null, true, true, BigDecimal.ZERO, 0L
        );

        given(favoriteRepository.findAllByUserId(userId, pageable))
                .willReturn(new PageImpl<>(List.of(favorite), pageable, 1));
        given(mapPlaceRepository.findAllByIds(Set.of(mapPlaceId)))
                .willReturn(Map.of(mapPlaceId, mapPlace));

        // when
        Page<FavoriteItemResponse> result = favoriteQueryService.getFavorites(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        FavoriteItemResponse item = result.getContent().get(0);
        assertThat(item.mapPlaceId()).isEqualTo(mapPlaceId);
        assertThat(item.name()).isEqualTo("테스트 장소");
        assertThat(item.district()).isEqualTo("마포구");
    }

    @Test
    @DisplayName("찜 목록이 없으면 빈 페이지를 반환한다")
    void getFavorites_returnsEmptyPage_whenNoFavorites() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        given(favoriteRepository.findAllByUserId(userId, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(mapPlaceRepository.findAllByIds(Set.of())).willReturn(Map.of());

        // when
        Page<FavoriteItemResponse> result = favoriteQueryService.getFavorites(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("찜한 장소의 찜 여부 조회 시 favorited=true를 반환한다")
    void getStatus_returnsTrue_whenFavorited() {
        // given
        Long userId = 1L;
        Long mapPlaceId = 10L;

        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.of(mock(MapPlace.class)));
        given(favoriteRepository.existsByUserIdAndMapPlaceId(userId, mapPlaceId)).willReturn(true);

        // when
        FavoriteStatusResponse response = favoriteQueryService.getStatus(userId, mapPlaceId);

        // then
        assertThat(response.favorited()).isTrue();
        assertThat(response.mapPlaceId()).isEqualTo(mapPlaceId);
    }

    @Test
    @DisplayName("찜하지 않은 장소의 찜 여부 조회 시 favorited=false를 반환한다")
    void getStatus_returnsFalse_whenNotFavorited() {
        // given
        Long userId = 1L;
        Long mapPlaceId = 10L;

        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.of(mock(MapPlace.class)));
        given(favoriteRepository.existsByUserIdAndMapPlaceId(userId, mapPlaceId)).willReturn(false);

        // when
        FavoriteStatusResponse response = favoriteQueryService.getStatus(userId, mapPlaceId);

        // then
        assertThat(response.favorited()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 장소의 찜 여부 조회 시 MAP_PLACE_NOT_FOUND 예외를 던진다")
    void getStatus_throwsException_whenMapPlaceNotFound() {
        // given
        Long mapPlaceId = 999L;
        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteQueryService.getStatus(1L, mapPlaceId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(FavoriteErrorCode.MAP_PLACE_NOT_FOUND));
    }

    @Test
    @DisplayName("다른 사용자의 찜 데이터가 조회되지 않는다")
    void getFavorites_doesNotReturnOtherUsersFavorites() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Pageable pageable = PageRequest.of(0, 20);

        // userId=1의 목록은 비어있음 (otherUserId=2의 찜이 섞이지 않음을 가정)
        given(favoriteRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(mapPlaceRepository.findAllByIds(Set.of())).willReturn(Map.of());

        // when
        Page<FavoriteItemResponse> result = favoriteQueryService.getFavorites(userId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    private MapPlace mock(Class<MapPlace> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
