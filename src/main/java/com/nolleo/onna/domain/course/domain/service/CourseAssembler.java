package com.nolleo.onna.domain.course.domain.service;

import com.nolleo.onna.domain.spot.domain.model.Spot;

import java.util.ArrayList;
import java.util.List;

/**
 * 선택된 스팟 목록을 시작 좌표 기준 최근접 탐욕(nearest-neighbor) 순서로 배치한다.
 * PostGIS 왕복 없이 이미 로딩된 좌표로 순수 계산한다.
 */
public class CourseAssembler {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private CourseAssembler() {
    }

    public record AssembledItem(Spot spot, short distanceFromPrevM) {
    }

    public static List<AssembledItem> assemble(double startLat, double startLon, List<Spot> candidates) {
        List<Spot> remaining = new ArrayList<>(candidates);
        List<AssembledItem> result = new ArrayList<>();
        double curLat = startLat;
        double curLon = startLon;

        while (!remaining.isEmpty()) {
            Spot nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Spot candidate : remaining) {
                double lat = candidate.getGeoCoordinate().latitude().doubleValue();
                double lon = candidate.getGeoCoordinate().longitude().doubleValue();
                double distance = haversineMeters(curLat, curLon, lat, lon);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = candidate;
                }
            }
            result.add(new AssembledItem(nearest, (short) Math.round(nearestDistance)));
            remaining.remove(nearest);
            curLat = nearest.getGeoCoordinate().latitude().doubleValue();
            curLon = nearest.getGeoCoordinate().longitude().doubleValue();
        }
        return result;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }
}
