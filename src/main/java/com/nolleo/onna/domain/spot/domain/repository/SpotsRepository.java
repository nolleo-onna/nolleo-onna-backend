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

    /** 주어진 content_id 목록 중 활성 스팟만 일괄 조회 — 새 참조를 만드는 쓰기 경로용 (조회 경로는 findByIds). */
    List<Spot> findActiveByIds(List<String> contentIds);

    /**
     * 활성 스팟 중 카테고리 목록(lcls_systm_1)에 속하고 기준점에서 radiusM(미터) 안에 있는 스팟을
     * 가까운 순으로 최대 limit개 조회. 정렬·절단은 DB(PostGIS KNN)가 담당하므로 호출자는 순서를 그대로 신뢰한다.
     */
    List<Spot> findNearbyByCategories(List<String> lclsSystm1Codes, double lat, double lon, double radiusM, int limit);

    /**
     * 활성 스팟 중 제목이 키워드를 포함하는 스팟을 "정확히 일치 → 기준점에서 가까운 순"으로 최대 limit개 조회.
     * 공백·대소문자를 무시하고 비교한다 ("광안리 해수욕장" ↔ "광안리해수욕장"). 사용자가 이름으로 지정한 스팟 매칭용.
     */
    List<Spot> findActiveByTitleNear(String title, double lat, double lon, int limit);

    /** 주어진 content_id 목록을 좌표 기준 거리순으로 정렬해서 조회. */
    List<Spot> findByIdsOrderByDistance(List<String> contentIds, double lat, double lon);

    /** 주어진 content_id 목록의 좌표로부터의 거리(미터)를 계산해서 반환. */
    Map<String, Integer> findDistancesFromPoint(List<String> contentIds, double lat, double lon);
}