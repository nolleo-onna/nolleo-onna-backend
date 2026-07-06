package com.nolleo.onna.domain.review.infrastructure.persistence;

import com.nolleo.onna.domain.review.domain.model.Review;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "rv_reviews",
        indexes = {
                @Index(name = "idx_rv_reviews_map_place_id", columnList = "map_place_id"),
                @Index(name = "idx_rv_reviews_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "map_place_id", nullable = false)
    private Long mapPlaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // rating 범위(1~5) 제약은 Alembic 마이그레이션의 CHECK 제약으로 관리
    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static ReviewEntity from(Review review) {
        return new ReviewEntity(
                null,
                review.mapPlaceId(),
                review.userId(),
                review.rating(),
                OffsetDateTime.now()
        );
    }

    public void updateRating(int newRating) {
        this.rating = newRating;
    }

    public Review toDomain() {
        return new Review(id, mapPlaceId, userId, rating, createdAt);
    }
}