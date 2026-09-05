package com.nolleo.onna.domain.spot.domain.model.vo;

import java.math.BigDecimal;

/** [값 객체] 지리 좌표 — 경도·위도는 항상 한 쌍으로만 유효함. */
public record GeoCoordinate(BigDecimal longitude, BigDecimal latitude) {

    public static GeoCoordinate of(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) return null;
        return new GeoCoordinate(longitude, latitude);
    }
}