package com.nolleo.onna.domain.spot.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.spot.domain.exception.SpotErrorCode;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.SpotDetail;
import com.nolleo.onna.domain.spot.domain.model.SpotImage;
import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;
import com.nolleo.onna.domain.spot.domain.repository.SpotDetailsRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotImagesRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotPriceSummaryRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotDetailResponse;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotMarkerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SpotQueryServiceTest {

    @Mock SpotsRepository spotsRepository;
    @Mock SpotDetailsRepository spotDetailsRepository;
    @Mock SpotImagesRepository spotImagesRepository;
    @Mock SpotPriceSummaryRepository spotPriceSummaryRepository;

    @InjectMocks SpotQueryService spotQueryService;

    @Test
    @DisplayName("활성 장소 목록을 마커 DTO 리스트로 반환한다")
    void getSpotMarkers_returnsActiveSpotsAsDtoList() {
        // given
        Spot mockSpot = mock(Spot.class);
        given(mockSpot.getContentId()).willReturn("1234567890");
        given(mockSpot.getContentTypeId()).willReturn("12");
        given(mockSpot.getTitle()).willReturn("해운대 해수욕장");
        given(mockSpot.getGeoCoordinate()).willReturn(
                GeoCoordinate.of(new BigDecimal("129.1603387"), new BigDecimal("35.1588473")));
        given(mockSpot.getFirstImage()).willReturn("https://example.com/img.jpg");
        given(mockSpot.getLclsSystm1()).willReturn("관광지");
        given(mockSpot.getLclsSystm2()).willReturn(null);
        given(mockSpot.getLclsSystm3()).willReturn(null);
        given(spotsRepository.findAllActive()).willReturn(List.of(mockSpot));

        // when
        List<SpotMarkerResponse> result = spotQueryService.getSpotMarkers();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).contentId()).isEqualTo("1234567890");
        assertThat(result.get(0).title()).isEqualTo("해운대 해수욕장");
        assertThat(result.get(0).mapX()).isEqualByComparingTo("129.1603387");
    }

    @Test
    @DisplayName("활성 장소가 없으면 빈 목록을 반환한다")
    void getSpotMarkers_returnsEmptyList_whenNoActiveSpots() {
        // given
        given(spotsRepository.findAllActive()).willReturn(List.of());

        // when
        List<SpotMarkerResponse> result = spotQueryService.getSpotMarkers();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("contentId로 활성 장소의 상세 정보를 반환한다")
    void getSpotDetail_returnsDetail_whenSpotIsActive() {
        // given
        String contentId = "1234567890";
        Spot mockSpot = buildDetailMockSpot(contentId);

        SpotDetail mockDetail = mock(SpotDetail.class);
        given(mockDetail.getTel()).willReturn("051-000-0000");
        given(mockDetail.getHomepage()).willReturn(null);
        given(mockDetail.getAddr1()).willReturn("부산광역시 해운대구");
        given(mockDetail.getAddr2()).willReturn(null);
        given(mockDetail.getZipcode()).willReturn("48094");
        given(mockDetail.getOverview()).willReturn("해운대 해수욕장 소개글");
        given(mockDetail.getParkingAvailable()).willReturn(true);

        given(spotsRepository.findById(contentId)).willReturn(Optional.of(mockSpot));
        given(spotDetailsRepository.findById(contentId)).willReturn(Optional.of(mockDetail));
        given(spotImagesRepository.findByContentId(contentId)).willReturn(List.of());
        given(spotPriceSummaryRepository.findById(contentId)).willReturn(Optional.empty());

        // when
        SpotDetailResponse result = spotQueryService.getSpotDetail(contentId);

        // then
        assertThat(result.contentId()).isEqualTo(contentId);
        assertThat(result.title()).isEqualTo("해운대 해수욕장");
        assertThat(result.addr1()).isEqualTo("부산광역시 해운대구");
        assertThat(result.tel()).isEqualTo("051-000-0000");
        assertThat(result.images()).isEmpty();
        assertThat(result.minPrice()).isNull();
    }

    @Test
    @DisplayName("SpotDetail과 SpotPriceSummary가 없어도 상세 조회가 정상 동작한다")
    void getSpotDetail_handlesAbsentOptionals_gracefully() {
        // given
        String contentId = "1234567890";
        Spot mockSpot = buildDetailMockSpot(contentId);

        given(spotsRepository.findById(contentId)).willReturn(Optional.of(mockSpot));
        given(spotDetailsRepository.findById(contentId)).willReturn(Optional.empty());
        given(spotImagesRepository.findByContentId(contentId)).willReturn(List.of());
        given(spotPriceSummaryRepository.findById(contentId)).willReturn(Optional.empty());

        // when
        SpotDetailResponse result = spotQueryService.getSpotDetail(contentId);

        // then
        assertThat(result.tel()).isNull();
        assertThat(result.addr1()).isNull();
        assertThat(result.overview()).isNull();
        assertThat(result.minPrice()).isNull();
        assertThat(result.images()).isEmpty();
    }

    @Test
    @DisplayName("이미지가 여러 장이면 모두 반환한다")
    void getSpotDetail_returnsAllImages() {
        // given
        String contentId = "1234567890";
        Spot mockSpot = buildDetailMockSpot(contentId);

        SpotImage img1 = mock(SpotImage.class);
        given(img1.getOriginImgUrl()).willReturn("https://example.com/img1.jpg");
        given(img1.getSmallImgUrl()).willReturn("https://example.com/img1_small.jpg");
        given(img1.getImgName()).willReturn("img1");

        SpotImage img2 = mock(SpotImage.class);
        given(img2.getOriginImgUrl()).willReturn("https://example.com/img2.jpg");
        given(img2.getSmallImgUrl()).willReturn(null);
        given(img2.getImgName()).willReturn("img2");

        given(spotsRepository.findById(contentId)).willReturn(Optional.of(mockSpot));
        given(spotDetailsRepository.findById(contentId)).willReturn(Optional.empty());
        given(spotImagesRepository.findByContentId(contentId)).willReturn(List.of(img1, img2));
        given(spotPriceSummaryRepository.findById(contentId)).willReturn(Optional.empty());

        // when
        SpotDetailResponse result = spotQueryService.getSpotDetail(contentId);

        // then
        assertThat(result.images()).hasSize(2);
        assertThat(result.images().get(0).originImgUrl()).isEqualTo("https://example.com/img1.jpg");
        assertThat(result.images().get(1).smallImgUrl()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 contentId 조회 시 SPOT_NOT_FOUND 예외를 던진다")
    void getSpotDetail_throwsException_whenSpotNotFound() {
        // given
        given(spotsRepository.findById("not-exist")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> spotQueryService.getSpotDetail("not-exist"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND));
    }

    @Test
    @DisplayName("비활성 장소 조회 시 SPOT_NOT_FOUND 예외를 던진다")
    void getSpotDetail_throwsException_whenSpotIsInactive() {
        // given
        Spot inactiveSpot = mock(Spot.class);
        given(inactiveSpot.isActive()).willReturn(false);
        given(spotsRepository.findById("1234567890")).willReturn(Optional.of(inactiveSpot));

        // when & then
        assertThatThrownBy(() -> spotQueryService.getSpotDetail("1234567890"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND));
    }

    // SpotDetailResponse.of()가 사용하는 전체 필드를 stub한 헬퍼
    private Spot buildDetailMockSpot(String contentId) {
        Spot spot = mock(Spot.class);
        given(spot.getContentId()).willReturn(contentId);
        given(spot.getContentTypeId()).willReturn("12");
        given(spot.getTitle()).willReturn("해운대 해수욕장");
        given(spot.getGeoCoordinate()).willReturn(
                GeoCoordinate.of(new BigDecimal("129.1603387"), new BigDecimal("35.1588473")));
        given(spot.getFirstImage()).willReturn("https://example.com/img.jpg");
        given(spot.getFirstImage2()).willReturn(null);
        given(spot.getLclsSystm1()).willReturn("관광지");
        given(spot.getLclsSystm2()).willReturn(null);
        given(spot.getLclsSystm3()).willReturn(null);
        given(spot.getLDongRegnCd()).willReturn("26");
        given(spot.getLDongSignguCd()).willReturn("26350");
        given(spot.isActive()).willReturn(true);
        return spot;
    }
}