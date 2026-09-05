package com.nolleo.onna.domain.map.infrastructure.persistence;

import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "mp_map_places",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_mp_map_places_type_original",
        columnNames = {"place_type", "original_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapPlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "place_type", nullable = false, length = 10)
    private PlaceType placeType;

    @Column(name = "original_id", nullable = false, length = 50)
    private String originalId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "district", length = 50)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 10)
    private PlaceCategory category;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "is_free", nullable = false)
    private boolean free;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "avg_rating", precision = 4, scale = 2, nullable = false)
    private BigDecimal avgRating;

    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @Column(name = "favorite_count", nullable = false)
    private long favoriteCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static MapPlaceEntity from(MapPlace mapPlace) {
        return new MapPlaceEntity(
            null,
            mapPlace.getPlaceType(),
            mapPlace.getOriginalId(),
            mapPlace.getName(),
            mapPlace.getDistrict(),
            mapPlace.getCategory(),
            mapPlace.getLongitude(),
            mapPlace.getLatitude(),
            mapPlace.getImageUrl(),
            mapPlace.getMinPrice(),
            mapPlace.isFree(),
            mapPlace.isActive(),
            mapPlace.getAvgRating(),
            mapPlace.getReviewCount(),
            mapPlace.getFavoriteCount(),
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }

    public void updateFrom(MapPlace mapPlace) {
        this.name      = mapPlace.getName();
        this.district  = mapPlace.getDistrict();
        this.category  = mapPlace.getCategory();
        this.longitude = mapPlace.getLongitude();
        this.latitude  = mapPlace.getLatitude();
        this.imageUrl  = mapPlace.getImageUrl();
        this.minPrice  = mapPlace.getMinPrice();
        this.free      = mapPlace.isFree();
        this.active    = mapPlace.isActive();
        this.updatedAt = OffsetDateTime.now();
    }

    public MapPlace toDomain() {
        return new MapPlace(id, placeType, originalId, name, district, category,
                            longitude, latitude, imageUrl, minPrice, free, active,
                            avgRating, reviewCount, favoriteCount);
    }
}