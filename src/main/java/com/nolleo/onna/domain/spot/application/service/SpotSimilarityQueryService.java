package com.nolleo.onna.domain.spot.application.service;

import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import com.nolleo.onna.domain.spot.domain.repository.SpotEmbeddingRepository;
import com.nolleo.onna.domain.spot.infrastructure.ai.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 무드/동행 쿼리 텍스트를 임베딩해 후보 스팟 풀 내에서 유사도순으로 재정렬한다.
 * 코스 생성 파이프라인의 벡터 리랭킹 단계에서 사용.
 */
@Service
@RequiredArgsConstructor
public class SpotSimilarityQueryService {

    private final OpenAiEmbeddingClient embeddingClient;
    private final SpotEmbeddingRepository spotEmbeddingRepository;

    /** queryText(무드·동행 설명)와 candidateContentIds 후보군을 유사도 내림차순으로 정렬해 반환 */
    public List<SpotSimilarity> rerankByQuery(String queryText, List<String> candidateContentIds) {
        List<Float> queryVector = embeddingClient.embed(queryText);
        return spotEmbeddingRepository.findSimilarWithin(queryVector, candidateContentIds);
    }
}
