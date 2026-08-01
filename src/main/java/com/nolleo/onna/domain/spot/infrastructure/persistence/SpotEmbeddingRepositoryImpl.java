package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.model.SpotSimilarity;
import com.nolleo.onna.domain.spot.domain.repository.SpotEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [인프라 어댑터] SpotEmbeddingRepository 도메인 포트의 구현체.
 * spot_embeddings.embedding 컬럼이 pgvector 타입이라 JPA 엔티티 매핑 대신
 * NamedParameterJdbcTemplate으로 직접 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class SpotEmbeddingRepositoryImpl implements SpotEmbeddingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<SpotSimilarity> findSimilarWithin(List<Float> queryVector, List<String> candidateContentIds) {
        if (candidateContentIds.isEmpty()) return List.of();

        String vectorLiteral = toVectorLiteral(queryVector);
        String sql = """
                SELECT content_id, 1 - (embedding <=> CAST(:vector AS vector)) AS similarity
                FROM vectors.spot_embeddings
                WHERE content_id IN (:ids)
                ORDER BY embedding <=> CAST(:vector AS vector)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("vector", vectorLiteral)
                .addValue("ids", candidateContentIds);

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new SpotSimilarity(rs.getString("content_id"), rs.getDouble("similarity")));
    }

    private String toVectorLiteral(List<Float> vector) {
        return vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
