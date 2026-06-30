package com.nolleo.onna.domain.review.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ReviewEntity r WHERE r.mapPlaceId = :mapPlaceId")
    double calculateAvgRating(@Param("mapPlaceId") Long mapPlaceId);

    long countByMapPlaceId(Long mapPlaceId);
}