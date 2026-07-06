package com.nolleo.onna.domain.map.domain.model;

import com.nolleo.onna.domain.map.domain.model.vo.PlaceCategory;
import com.nolleo.onna.domain.map.domain.model.vo.PlaceType;

import java.math.BigDecimal;

public class MapPlace {

    private final Long id;
    private final PlaceType placeType;
    private final String originalId;
    private final String name;
    private final String district;
    private final PlaceCategory category;
    private final BigDecimal longitude;
    private final BigDecimal latitude;
    private final String imageUrl;
    private final Integer minPrice;
    private final boolean free;
    private final boolean active;
    private final BigDecimal avgRating;
    private final long reviewCount;

    public MapPlace(Long id, PlaceType placeType, String originalId, String name,
                    String district, PlaceCategory category,
                    BigDecimal longitude, BigDecimal latitude,
                    String imageUrl, Integer minPrice, boolean free, boolean active,
                    BigDecimal avgRating, long reviewCount) {
        this.id = id;
        this.placeType = placeType;
        this.originalId = originalId;
        this.name = name;
        this.district = district;
        this.category = category;
        this.longitude = longitude;
        this.latitude = latitude;
        this.imageUrl = imageUrl;
        this.minPrice = minPrice;
        this.free = free;
        this.active = active;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

    public Long getId()             { return id; }
    public PlaceType getPlaceType() { return placeType; }
    public String getOriginalId()   { return originalId; }
    public String getName()         { return name; }
    public String getDistrict()     { return district; }
    public PlaceCategory getCategory() { return category; }
    public BigDecimal getLongitude()   { return longitude; }
    public BigDecimal getLatitude()    { return latitude; }
    public String getImageUrl()        { return imageUrl; }
    public Integer getMinPrice()       { return minPrice; }
    public boolean isFree()            { return free; }
    public boolean isActive()          { return active; }
    public BigDecimal getAvgRating()   { return avgRating; }
    public long getReviewCount()       { return reviewCount; }
}