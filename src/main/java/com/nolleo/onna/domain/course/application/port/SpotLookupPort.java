package com.nolleo.onna.domain.course.application.port;

import com.nolleo.onna.domain.course.application.dto.SpotCandidate;

import java.util.List;
import java.util.Map;

/**
 * [아웃바운드 포트] Spot 컨텍스트로부터 코스 생성/조회에 필요한 스팟 정보를 조회한다.
 *
 * Course 컨텍스트가 Spot 컨텍스트의 도메인 모델·리포지토리에 직접 의존하지 않도록 하는 경계.
 * 구현(어댑터)은 infrastructure/spot에 위치하며, 그 안에서만 Spot 컨텍스트를 참조한다.
 */
public interface SpotLookupPort {

    /** 좌표 기준 거리순으로 카테고리에 해당하는 후보 스팟을 조회 */
    List<SpotCandidate> findNearbyByCategory(String categoryCode, double lat, double lon);

    /** content_id 목록으로 스팟을 일괄 조회 — 활성 여부와 무관 (이미 담긴 스팟을 보여주는 조회 경로용) */
    Map<String, SpotCandidate> findByIds(List<String> contentIds);

    /**
     * content_id 목록 중 활성 스팟만 일괄 조회 — 코스에 새로 담는 쓰기 경로용.
     * 비활성 스팟은 결과 맵에서 빠진다. 조회 경로(findByIds)는 담긴 뒤 비활성화된 스팟도 보여줘야 하므로 필터가 없다.
     */
    Map<String, SpotCandidate> findActiveByIds(List<String> contentIds);

    /** 음식점 카테고리 스팟의 예상 방문 비용(원)을 일괄 조회 — 값이 없으면 결과 맵에서 제외 */
    Map<String, Integer> findFoodPrices(List<String> foodContentIds);
}
