package com.nolleo.onna.domain.course.application.port;

import java.util.List;

/**
 * [아웃바운드 포트] 후보 스팟을 쿼리 텍스트(무드·동행) 기준 관련도순으로 재정렬.
 *
 * Course 컨텍스트가 Spot 컨텍스트의 유스케이스에 직접 의존하지 않도록 하는 경계.
 * 구현(어댑터)은 infrastructure/spot에 위치한다.
 */
public interface SpotReranker {

    /**
     * queryText를 한 번 준비(임베딩)해 두고 여러 후보 목록에 반복 적용할 수 있는 랭커를 만든다.
     * 임베딩 API 호출은 이 메서드에서만 일어나므로, 코스 생성 1회당 1번만 호출한다.
     */
    Ranker prepare(String queryText);

    /** 준비된 쿼리 기준으로 후보를 정렬하는 랭커 — 같은 쿼리로 여러 카테고리 그룹을 정렬할 때 재사용한다 */
    @FunctionalInterface
    interface Ranker {

        /** candidateIds를 쿼리 관련도 내림차순으로 정렬한 식별자 목록으로 반환 */
        List<String> rerank(List<String> candidateIds);
    }
}
