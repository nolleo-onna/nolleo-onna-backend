package com.nolleo.onna.domain.spot.application.port;

import java.util.List;

/**
 * [아웃바운드 포트] 텍스트 → 임베딩 벡터 변환.
 * 구현은 infrastructure/ai에 위치한다.
 *
 * spot_embeddings 테이블과 동일한 벡터 공간을 유지해야 하므로
 * 구현체는 적재 시 사용한 모델과 같은 모델을 써야 한다.
 */
public interface EmbeddingClient {

    /** 텍스트를 임베딩 벡터로 변환 */
    List<Float> embed(String text);
}
