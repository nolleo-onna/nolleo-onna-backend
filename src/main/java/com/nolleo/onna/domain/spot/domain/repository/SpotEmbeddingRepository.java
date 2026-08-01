package com.nolleo.onna.domain.spot.domain.repository;

import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;

import java.util.List;

/**
 * [도메인 포트] spot_embeddings 저장소 인터페이스.
 * 구현체: infrastructure/persistence/SpotEmbeddingRepositoryImpl
 */
public interface SpotEmbeddingRepository {

    /**
     * 주어진 content_id 후보군 내에서 쿼리 벡터와의 코사인 유사도를 계산해
     * 유사도 내림차순으로 정렬해 반환한다.
     */
    List<SpotSimilarity> findSimilarWithin(List<Float> queryVector, List<String> candidateContentIds);
}
