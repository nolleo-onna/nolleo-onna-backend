package com.nolleo.onna.domain.course.infrastructure.spot;

import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.spot.application.service.SpotSimilarityQueryService;
import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SpotReranker 포트의 어댑터.
 * prepare에서 쿼리 텍스트를 한 번만 임베딩해 벡터를 붙잡아 두고,
 * 돌려준 Ranker가 그 벡터로 Spot 컨텍스트의 유사도 검색 결과(SpotSimilarity)를
 * Course 컨텍스트가 이해하는 식별자 목록으로 변환한다.
 *
 * 리랭킹은 품질을 높이는 단계이지 코스 생성의 전제가 아니다. 임베딩 API나 벡터 조회가 실패하면
 * 로그만 남기고 후보를 입력 순서(거리순) 그대로 돌려주는 랭커로 폴백해 생성이 끊기지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotSimilarityRerankerAdapter implements SpotReranker {

    private final SpotSimilarityQueryService spotSimilarityQueryService;

    @Override
    public Ranker prepare(String queryText) {
        List<Float> queryVector;
        try {
            queryVector = spotSimilarityQueryService.embedQuery(queryText);
        } catch (RuntimeException e) {
            log.warn("쿼리 임베딩 실패 — 거리순으로 폴백 queryText={}: {}", queryText, e.getMessage());
            return candidateIds -> candidateIds;
        }
        return candidateIds -> {
            try {
                return spotSimilarityQueryService.rerankWithin(queryVector, candidateIds).stream()
                        .map(SpotSimilarity::contentId)
                        .toList();
            } catch (RuntimeException e) {
                log.warn("벡터 리랭킹 실패 — 거리순으로 폴백: {}", e.getMessage());
                return candidateIds;
            }
        };
    }
}
