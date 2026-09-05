package com.nolleo.onna.domain.course.domain.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 선택된 방문 지점을 시작 좌표 기준 최근접 탐욕(nearest-neighbor) 순서로 배치한다.
 * PostGIS 왕복 없이 이미 로딩된 좌표로 순수 계산한다.
 *
 * Spot 애그리거트를 직접 알지 않고 Course 컨텍스트 소유의 Waypoint VO만 다룬다.
 * Spot → Waypoint 변환은 application 계층의 책임이다.
 */
public class CourseAssembler {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private CourseAssembler() {
    }

    /**
     * 코스 조립에 필요한 최소 정보만 담은 방문 지점.
     *   refId — 외부 애그리거트 식별자 (현재는 spot content_id)
     */
    public record Waypoint(String refId, double latitude, double longitude) {
    }

    public record AssembledItem(Waypoint waypoint, short distanceFromPrevM) {
    }

    public static List<AssembledItem> assemble(double startLat, double startLon, List<Waypoint> candidates) {
        List<Waypoint> remaining = new ArrayList<>(candidates);
        List<AssembledItem> result = new ArrayList<>();
        double curLat = startLat;
        double curLon = startLon;

        while (!remaining.isEmpty()) {
            Waypoint nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Waypoint candidate : remaining) {
                double distance = haversineMeters(curLat, curLon, candidate.latitude(), candidate.longitude());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = candidate;
                }
            }
            result.add(new AssembledItem(nearest, (short) Math.round(nearestDistance)));
            remaining.remove(nearest);
            curLat = nearest.latitude();
            curLon = nearest.longitude();
        }
        return result;
    }

    /** 두 좌표 사이의 대권 거리(미터). 후보 정렬 등 조립 외 용도로도 사용한다. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        return haversineMeters(lat1, lon1, lat2, lon2);
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
