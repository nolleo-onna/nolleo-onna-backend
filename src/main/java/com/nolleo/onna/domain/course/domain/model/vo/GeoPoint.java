package com.nolleo.onna.domain.course.domain.model.vo;

/** WGS84 좌표 — Course 컨텍스트가 검색 중심·거리 계산에 쓰는 값 객체 */
public record GeoPoint(double latitude, double longitude) {
}
