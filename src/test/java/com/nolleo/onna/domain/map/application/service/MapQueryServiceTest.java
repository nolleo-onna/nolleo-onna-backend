package com.nolleo.onna.domain.map.application.service;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import com.nolleo.onna.domain.map.presentation.dto.response.MapPlaceResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MapQueryServiceTest {

    @Mock MapPlaceRepository mapPlaceRepository;

    @InjectMocks MapQueryService mapQueryService;

    private final Pageable pageable = PageRequest.of(0, 20);

    private MapPlace samplePlace(PlaceCategory category) {
        return new MapPlace(
                1L, PlaceType.SPOT, "orig-1", "테스트 장소", "해운대구", category,
                new BigDecimal("129.160"), new BigDecimal("35.158"),
                null, null, true, true, new BigDecimal("0.00"), 0L, 0L
        );
    }

    @Test
    @DisplayName("keyword가 null이면 repository에 null을 전달한다")
    void getMapPlaces_nullKeyword_passesNullToRepository() {
        given(mapPlaceRepository.findByFilterPaged(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(samplePlace(PlaceCategory.VE))));

        mapQueryService.getMapPlaces(null, null, null, null, pageable);

        verify(mapPlaceRepository).findByFilterPaged(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("keyword가 빈 문자열이면 repository에 null을 전달한다")
    void getMapPlaces_emptyKeyword_passesNullToRepository() {
        given(mapPlaceRepository.findByFilterPaged(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(samplePlace(PlaceCategory.VE))));

        mapQueryService.getMapPlaces(null, null, null, "", pageable);

        verify(mapPlaceRepository).findByFilterPaged(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("keyword가 공백만 있으면 repository에 null을 전달한다")
    void getMapPlaces_blankKeyword_passesNullToRepository() {
        given(mapPlaceRepository.findByFilterPaged(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(samplePlace(PlaceCategory.VE))));

        mapQueryService.getMapPlaces(null, null, null, "   ", pageable);

        verify(mapPlaceRepository).findByFilterPaged(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("keyword의 앞뒤 공백은 trim되어 repository에 전달된다")
    void getMapPlaces_keywordWithSpaces_passesTrimmedKeyword() {
        given(mapPlaceRepository.findByFilterPaged(isNull(), isNull(), isNull(), eq("카페"), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(samplePlace(PlaceCategory.FD))));

        mapQueryService.getMapPlaces(null, null, null, "  카페  ", pageable);

        verify(mapPlaceRepository).findByFilterPaged(null, null, null, "카페", pageable);
    }

    @Test
    @DisplayName("유효한 keyword는 그대로 repository에 전달된다")
    void getMapPlaces_validKeyword_passesKeywordToRepository() {
        given(mapPlaceRepository.findByFilterPaged(isNull(), isNull(), isNull(), eq("해운대"), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(samplePlace(PlaceCategory.NA))));

        mapQueryService.getMapPlaces(null, null, null, "해운대", pageable);

        verify(mapPlaceRepository).findByFilterPaged(null, null, null, "해운대", pageable);
    }

    @Test
    @DisplayName("getMapPlaces 결과는 MapPlaceResponse로 변환되어 반환된다")
    void getMapPlaces_returnsMappedPage() {
        MapPlace place = samplePlace(PlaceCategory.VE);
        given(mapPlaceRepository.findByFilterPaged(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(place)));

        Page<MapPlaceResponse> result = mapQueryService.getMapPlaces(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("테스트 장소");
        assertThat(result.getContent().get(0).category()).isEqualTo(PlaceCategory.VE);
    }
}
