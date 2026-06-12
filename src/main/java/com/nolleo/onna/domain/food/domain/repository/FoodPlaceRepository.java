package com.nolleo.onna.domain.food.domain.repository;

import com.nolleo.onna.domain.food.domain.model.FoodPlace;

import java.util.List;
import java.util.Optional;

/**
 * [도메인 포트] FoodPlace 저장소 인터페이스.
 * 구현체: infrastructure/persistence/FoodPlaceRepositoryImpl
 */
public interface FoodPlaceRepository {

    /** ID로 단건 조회 (메뉴 포함) */
    Optional<FoodPlace> findById(Long id);

    /** 활성 장소 전체 조회 */
    List<FoodPlace> findAllActive();

    /** 표준 업종 분류별 활성 장소 조회 */
    List<FoodPlace> findAllByNormalizedCategory(String normalizedCategory);

    /** 코스 식사 후보 장소 조회 */
    List<FoodPlace> findAllCourseFoodCandidates();

    /** 지역별 활성 장소 조회 */
    List<FoodPlace> findAllBySourceRegion(String sourceRegion);
}
