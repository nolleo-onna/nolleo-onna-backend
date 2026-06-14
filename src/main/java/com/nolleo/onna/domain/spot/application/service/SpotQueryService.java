package com.nolleo.onna.domain.spot.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.spot.domain.exception.SpotErrorCode;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.model.SpotDetail;
import com.nolleo.onna.domain.spot.domain.model.SpotImage;
import com.nolleo.onna.domain.spot.domain.model.SpotPriceSummary;
import com.nolleo.onna.domain.spot.domain.repository.SpotDetailsRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotImagesRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotPriceSummaryRepository;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotDetailResponse;
import com.nolleo.onna.domain.spot.presentation.dto.response.SpotMarkerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotQueryService {

    private final SpotsRepository spotsRepository;
    private final SpotDetailsRepository spotDetailsRepository;
    private final SpotImagesRepository spotImagesRepository;
    private final SpotPriceSummaryRepository spotPriceSummaryRepository;

    public List<SpotMarkerResponse> getSpotMarkers() {
        return spotsRepository.findAllActive().stream()
                .map(SpotMarkerResponse::from)
                .toList();
    }

    public SpotDetailResponse getSpotDetail(String contentId) {
        Spot spot = spotsRepository.findById(contentId)
                .filter(Spot::isActive)
                .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        SpotDetail detail = spotDetailsRepository.findById(contentId).orElse(null);
        List<SpotImage> images = spotImagesRepository.findByContentId(contentId);
        SpotPriceSummary priceSummary = spotPriceSummaryRepository.findById(contentId).orElse(null);

        return SpotDetailResponse.of(spot, detail, images, priceSummary);
    }
}