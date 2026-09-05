package com.nolleo.onna.domain.course.infrastructure.spot;

import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.spot.application.service.SpotSimilarityQueryService;
import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SpotReranker 포트의 어댑터.
 * Spot 컨텍스트의 벡터 유사도 검색 결과(SpotSimilarity)를
 * Course 컨텍스트가 이해하는 식별자 목록으로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class SpotSimilarityRerankerAdapter implements SpotReranker {

    private final SpotSimilarityQueryService spotSimilarityQueryService;

    @Override
    public List<String> rerank(String queryText, List<String> candidateIds) {
        return spotSimilarityQueryService.rerankByQuery(queryText, candidateIds).stream()
                .map(SpotSimilarity::contentId)
                .toList();
    }
}
