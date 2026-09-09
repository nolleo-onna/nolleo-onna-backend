package com.nolleo.onna.domain.course.domain.service;

import com.nolleo.onna.domain.course.domain.service.CourseAssembler.AssembledItem;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler.Waypoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseAssemblerTest {

    // 광안리 중심
    private static final double START_LAT = 35.1531;
    private static final double START_LON = 129.1187;

    @Test
    @DisplayName("시작점에서 가까운 순으로 최근접 탐욕 배치한다 — 입력 순서와 무관하다")
    void assemble_ordersByNearestNeighbor() {
        Waypoint far  = new Waypoint("far",  35.1700, 129.1400);   // ~2.8km
        Waypoint near = new Waypoint("near", 35.1540, 129.1190);   // ~100m
        Waypoint mid  = new Waypoint("mid",  35.1600, 129.1250);   // ~1km

        List<AssembledItem> result = CourseAssembler.assemble(START_LAT, START_LON, List.of(far, near, mid));

        assertThat(result).extracting(item -> item.waypoint().refId())
                .containsExactly("near", "mid", "far");
    }

    @Test
    @DisplayName("첫 지점은 시작 좌표 기준, 이후는 직전 지점 기준으로 거리를 잰다")
    void assemble_measuresDistanceFromPrevious() {
        Waypoint a = new Waypoint("a", 35.1540, 129.1190);
        Waypoint b = new Waypoint("b", 35.1600, 129.1250);

        List<AssembledItem> result = CourseAssembler.assemble(START_LAT, START_LON, List.of(a, b));

        int expectedFirst  = (int) Math.round(CourseAssembler.distanceMeters(START_LAT, START_LON, a.latitude(), a.longitude()));
        int expectedSecond = (int) Math.round(CourseAssembler.distanceMeters(a.latitude(), a.longitude(), b.latitude(), b.longitude()));
        assertThat(result.get(0).distanceFromPrevM()).isEqualTo(expectedFirst);
        assertThat(result.get(1).distanceFromPrevM()).isEqualTo(expectedSecond);
    }

    @Test
    @DisplayName("후보가 없으면 빈 결과를 반환한다")
    void assemble_returnsEmpty_whenNoCandidates() {
        assertThat(CourseAssembler.assemble(START_LAT, START_LON, List.of())).isEmpty();
    }

    @Test
    @DisplayName("부산 최장 구간(기장↔가덕도)처럼 32,767m를 넘는 거리도 int로 정상 보관한다")
    void assemble_holdsDistanceBeyondShortRange() {
        Waypoint gijang  = new Waypoint("gijang",  35.2444, 129.2144);
        Waypoint gadeok  = new Waypoint("gadeok",  35.0500, 128.8300);

        List<AssembledItem> result = CourseAssembler.assemble(gijang.latitude(), gijang.longitude(), List.of(gadeok));

        assertThat(result.get(0).distanceFromPrevM()).isGreaterThan(Short.MAX_VALUE);
    }
}
