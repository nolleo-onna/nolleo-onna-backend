package com.nolleo.onna.domain.course.application.port;

import java.util.List;

/**
 * [아웃바운드 포트] 후보 스팟을 쿼리 텍스트(무드·동행) 기준 관련도순으로 재정렬.
 *
 * Course 컨텍스트가 Spot 컨텍스트의 유스케이스에 직접 의존하지 않도록 하는 경계.
 * 구현(어댑터)은 infrastructure/spot에 위치한다.
 */
public interface SpotReranker {

    /** candidateIds를 queryText 관련도 내림차순으로 정렬한 식별자 목록으로 반환 */
    List<String> rerank(String queryText, List<String> candidateIds);
}
