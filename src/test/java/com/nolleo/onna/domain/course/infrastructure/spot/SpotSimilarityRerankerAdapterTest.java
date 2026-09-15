package com.nolleo.onna.domain.course.infrastructure.spot;

import com.nolleo.onna.domain.course.application.port.SpotReranker;
import com.nolleo.onna.domain.spot.application.service.SpotSimilarityQueryService;
import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import com.nolleo.onna.domain.spot.infrastructure.ai.OpenAiEmbeddingClient.OpenAiApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpotSimilarityRerankerAdapterTest {

    @Mock SpotSimilarityQueryService spotSimilarityQueryService;

    @InjectMocks SpotSimilarityRerankerAdapter adapter;

    private static final String QUERY = "로맨틱 연인 여행";
    private static final List<Float> VECTOR = List.of(0.1f, 0.2f);
    private static final List<String> POOL = List.of("a", "b", "c");

    @Test
    @DisplayName("임베딩은 prepare에서 한 번만 하고, 랭커는 유사도 내림차순 식별자 목록을 돌려준다")
    void prepare_embedsOnce_andRankerMapsSimilarityOrder() {
        // given
        given(spotSimilarityQueryService.embedQuery(QUERY)).willReturn(VECTOR);
        given(spotSimilarityQueryService.rerankWithin(VECTOR, POOL))
                .willReturn(List.of(new SpotSimilarity("c", 0.9), new SpotSimilarity("a", 0.5)));

        // when
        SpotReranker.Ranker ranker = adapter.prepare(QUERY);
        List<String> ranked = ranker.rerank(POOL);

        // then
        assertThat(ranked).containsExactly("c", "a");
        verify(spotSimilarityQueryService).embedQuery(QUERY);
    }

    @Test
    @DisplayName("임베딩 API가 실패하면 예외를 올리지 않고 입력 순서(거리순)를 그대로 돌려주는 랭커로 폴백한다")
    void prepare_fallsBackToInputOrder_whenEmbeddingFails() {
        // given
        given(spotSimilarityQueryService.embedQuery(QUERY)).willThrow(new OpenAiApiException("OpenAI 호출 실패"));

        // when
        SpotReranker.Ranker ranker = adapter.prepare(QUERY);

        // then
        assertThat(ranker.rerank(POOL)).containsExactly("a", "b", "c");
        verify(spotSimilarityQueryService, never()).rerankWithin(anyList(), anyList());
    }

    @Test
    @DisplayName("벡터 조회가 실패하면 그 호출만 입력 순서로 폴백한다")
    void ranker_fallsBackToInputOrder_whenVectorQueryFails() {
        // given
        given(spotSimilarityQueryService.embedQuery(QUERY)).willReturn(VECTOR);
        given(spotSimilarityQueryService.rerankWithin(VECTOR, POOL))
                .willThrow(new QueryTimeoutException("pgvector timeout"));

        // when
        List<String> ranked = adapter.prepare(QUERY).rerank(POOL);

        // then
        assertThat(ranked).containsExactly("a", "b", "c");
    }
}
