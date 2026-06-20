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

    /** 활성 음식점 전체 조회 (지도 마커용 — 메뉴 미포함) */
    List<FoodPlace> findAllActive();

    /** 활성 음식점 전체 조회 (메뉴 포함 — 동기화용) */
    List<FoodPlace> findAllActiveWithMenus();
}
