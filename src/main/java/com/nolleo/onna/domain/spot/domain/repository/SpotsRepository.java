package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.model.Spot;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * [도메인 포트] Spot 저장소 인터페이스.
 * 구현체: infrastructure/persistence/SpotRepositoryImpl
 */
public interface SpotsRepository {

    Optional<Spot> findById(String contentId);

    List<Spot> findAll();

    List<Spot> findAllActive();

    /** 주어진 content_id 목록에 해당하는 스팟을 순서 무관하게 일괄 조회 (배치 조회용). */
    List<Spot> findByIds(List<String> contentIds);

    /** 활성 스팟 중 좌표 기준 거리순 조회. lclsSystm1이 null이면 카테고리 제한 없이 조회. */
    List<Spot> findNearbyByCategory(String lclsSystm1, double lat, double lon);

    /** 주어진 content_id 목록을 좌표 기준 거리순으로 정렬해서 조회. */
    List<Spot> findByIdsOrderByDistance(List<String> contentIds, double lat, double lon);

    /** 주어진 content_id 목록의 좌표로부터의 거리(미터)를 계산해서 반환. */
    Map<String, Integer> findDistancesFromPoint(List<String> contentIds, double lat, double lon);
}