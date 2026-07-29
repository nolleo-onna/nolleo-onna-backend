package com.nolleo.onna.domain.favorite.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.favorite.domain.exception.FavoriteErrorCode;
import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import com.nolleo.onna.domain.favorite.domain.repository.FavoriteRepository;
import com.nolleo.onna.domain.favorite.presentation.dto.response.FavoriteToggleResponse;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteCommandServiceTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock MapPlaceRepository mapPlaceRepository;

    @InjectMocks FavoriteCommandService favoriteCommandService;

    @Test
    @DisplayName("찜하지 않은 장소를 토글하면 찜 데이터가 저장되고 favorited=true를 반환한다")
    void toggle_saveFavorite_whenNotFavorited() {
        // given
        Long userId = 1L;
        Long mapPlaceId = 10L;

        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.of(mock(MapPlace.class)));
        given(favoriteRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId)).willReturn(Optional.empty());
        given(favoriteRepository.save(any(Favorite.class)))
                .willReturn(new Favorite(1L, userId, mapPlaceId, OffsetDateTime.now()));

        // when
        FavoriteToggleResponse response = favoriteCommandService.toggle(userId, mapPlaceId);

        // then
        assertThat(response.favorited()).isTrue();
        assertThat(response.mapPlaceId()).isEqualTo(mapPlaceId);
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    @DisplayName("이미 찜한 장소를 토글하면 찜 데이터가 삭제되고 favorited=false를 반환한다")
    void toggle_deleteFavorite_whenAlreadyFavorited() {
        // given
        Long userId = 1L;
        Long mapPlaceId = 10L;
        Favorite existing = new Favorite(1L, userId, mapPlaceId, OffsetDateTime.now());

        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.of(mock(MapPlace.class)));
        given(favoriteRepository.findByUserIdAndMapPlaceId(userId, mapPlaceId)).willReturn(Optional.of(existing));

        // when
        FavoriteToggleResponse response = favoriteCommandService.toggle(userId, mapPlaceId);

        // then
        assertThat(response.favorited()).isFalse();
        assertThat(response.mapPlaceId()).isEqualTo(mapPlaceId);
        verify(favoriteRepository, times(1)).delete(existing);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("존재하지 않는 장소를 찜하면 MAP_PLACE_NOT_FOUND 예외를 던진다")
    void toggle_throwsException_whenMapPlaceNotFound() {
        // given
        Long mapPlaceId = 999L;
        given(mapPlaceRepository.findById(mapPlaceId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteCommandService.toggle(1L, mapPlaceId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(FavoriteErrorCode.MAP_PLACE_NOT_FOUND));

        verify(favoriteRepository, never()).save(any());
        verify(favoriteRepository, never()).delete(any());
    }
}
