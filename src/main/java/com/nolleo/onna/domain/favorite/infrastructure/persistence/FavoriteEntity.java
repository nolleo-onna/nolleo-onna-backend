package com.nolleo.onna.domain.favorite.infrastructure.persistence;

import com.nolleo.onna.domain.favorite.domain.model.Favorite;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "fv_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_fv_favorites_user_map_place",
                columnNames = {"user_id", "map_place_id"}
        ),
        indexes = {
                @Index(name = "idx_fv_favorites_user_id", columnList = "user_id"),
                @Index(name = "idx_fv_favorites_map_place_id", columnList = "map_place_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FavoriteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "map_place_id", nullable = false)
    private Long mapPlaceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static FavoriteEntity from(Favorite favorite) {
        return new FavoriteEntity(
                null,
                favorite.userId(),
                favorite.mapPlaceId(),
                OffsetDateTime.now()
        );
    }

    public Favorite toDomain() {
        return new Favorite(id, userId, mapPlaceId, createdAt);
    }
}
