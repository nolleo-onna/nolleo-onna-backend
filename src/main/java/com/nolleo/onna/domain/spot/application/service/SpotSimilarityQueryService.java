package com.nolleo.onna.domain.spot.application.service;

import com.nolleo.onna.domain.spot.application.port.EmbeddingClient;
import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import com.nolleo.onna.domain.spot.domain.repository.SpotEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 무드/동행 쿼리 텍스트를 임베딩해 후보 스팟 풀 내에서 유사도순으로 재정렬한다.
 * 코스 생성 파이프라인의 벡터 리랭킹 단계에서 사용.
 *
 * 임베딩(embedQuery)과 정렬(rerankWithin)을 분리한 이유 — 한 생성 요청이 카테고리 그룹마다 같은 쿼리로
 * 정렬을 반복하므로, 벡터를 한 번 만들어 재사용하면 임베딩 API 호출이 그룹 수만큼 늘지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SpotSimilarityQueryService {

    private final EmbeddingClient embeddingClient;
    private final SpotEmbeddingRepository spotEmbeddingRepository;

    /** queryText(무드·동행 설명)를 spot_embeddings와 같은 벡터 공간의 쿼리 벡터로 변환 */
    public List<Float> embedQuery(String queryText) {
        return embeddingClient.embed(queryText);
    }

    /** 미리 만든 queryVector 기준으로 candidateContentIds 후보군을 유사도 내림차순으로 정렬해 반환 */
    public List<SpotSimilarity> rerankWithin(List<Float> queryVector, List<String> candidateContentIds) {
        return spotEmbeddingRepository.findSimilarWithin(queryVector, candidateContentIds);
    }
}
