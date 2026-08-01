package com.nolleo.onna.domain.spot.domain.model;

/** 쿼리 벡터와의 코사인 유사도 검색 결과 (spot_embeddings 조회 전용). */
public record SpotSimilarity(String contentId, double similarity) {
}
