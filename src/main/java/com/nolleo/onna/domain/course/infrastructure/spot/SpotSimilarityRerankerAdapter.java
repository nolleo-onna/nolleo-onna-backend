package com.nolleo.onna.domain.course.infrastructure.spot;

import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.spot.application.service.SpotSimilarityQueryService;
import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SpotReranker 포트의 어댑터.
 * prepare에서 쿼리 텍스트를 한 번만 임베딩해 벡터를 붙잡아 두고,
 * 돌려준 Ranker가 그 벡터로 Spot 컨텍스트의 유사도 검색 결과(SpotSimilarity)를
 * Course 컨텍스트가 이해하는 식별자 목록으로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class SpotSimilarityRerankerAdapter implements SpotReranker {

    private final SpotSimilarityQueryService spotSimilarityQueryService;

    @Override
    public Ranker prepare(String queryText) {
        List<Float> queryVector = spotSimilarityQueryService.embedQuery(queryText);
        return candidateIds -> spotSimilarityQueryService.rerankWithin(queryVector, candidateIds).stream()
                .map(SpotSimilarity::contentId)
                .toList();
    }
}
