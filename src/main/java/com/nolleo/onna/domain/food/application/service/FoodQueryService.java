package com.nolleo.onna.domain.food.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.food.domain.exception.FoodErrorCode;
import com.nolleo.onna.domain.food.domain.model.FoodPlace;
import com.nolleo.onna.domain.food.domain.repository.FoodPlaceRepository;
import com.nolleo.onna.domain.food.presentation.dto.response.FoodPlaceDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodQueryService {

    private final FoodPlaceRepository foodPlaceRepository;

    public FoodPlaceDetailResponse getFoodPlaceDetail(Long id) {
        FoodPlace food = foodPlaceRepository.findById(id)
                .filter(FoodPlace::isActive)
                .orElseThrow(() -> new BusinessException(FoodErrorCode.FOOD_NOT_FOUND));

        return FoodPlaceDetailResponse.from(food);
    }
}
