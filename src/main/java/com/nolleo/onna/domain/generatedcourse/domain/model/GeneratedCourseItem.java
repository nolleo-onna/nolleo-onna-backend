package com.nolleo.onna.domain.generatedcourse.domain.model;

import com.nolleo.onna.domain.generatedcourse.domain.model.vo.WeatherWarning;
import lombok.Getter;

import java.time.LocalTime;

/**
 * 코스에 포함된 방문 장소 엔티티 (GeneratedCourse의 자식 엔티티).
 * 외부에서 직접 변경할 수 없으며, 반드시 GeneratedCourse(Aggregate Root)를 통해서만 상태가 변경된다.
 */
@Getter
public class GeneratedCourseItem {

    /** 내부 코스 아이템 식별자 (PK) */
    private Long id;

    /** 소속 코스 ID (FK) */
    private Long courseId;

    /** 코스 내 방문 순서 (1부터 시작) */
    private final Short serialNum;

    /** mp_map_places.id 참조 */
    private final Long mapPlaceId;

    /** 예상 도착 시각 */
    private LocalTime arrivalTime;

    /** 예상 체류 시간 (분) */
    private Short durationMinutes;

    /** 예상 방문 비용 (원) */
    private Integer expectedCost;

    /** 이전 장소로부터 직선 거리 (미터) */
    private Short distanceFromPrevM;

    /** 타임라인에 표시할 메모 */
    private String notes;

    /** 날씨 경고 정보 묶음 (경고여부·메시지) */
    private WeatherWarning weatherWarning;

    /** 신규 생성용 생성자 — GeneratedCourse.addItem()에서만 호출 */
    GeneratedCourseItem(Short serialNum, Long mapPlaceId,
                        short durationMinutes, short distanceFromPrevM) {
        this.id = null;
        this.courseId = null;
        this.serialNum = serialNum;
        this.mapPlaceId = mapPlaceId;
        this.durationMinutes = durationMinutes;
        this.distanceFromPrevM = distanceFromPrevM;
        this.weatherWarning = WeatherWarning.none();
    }

    /**
     * DB에서 읽어온 데이터로 도메인 객체를 재구성한다.
     * Repository 구현체에서 DB 조회 후 호출한다.
     */
    public static GeneratedCourseItem restore(Long id, Long courseId, Short serialNum,
                                               Long mapPlaceId, LocalTime arrivalTime,
                                               Short durationMinutes, Integer expectedCost,
                                               Short distanceFromPrevM, String notes,
                                               WeatherWarning weatherWarning) {
        GeneratedCourseItem item = new GeneratedCourseItem(serialNum, mapPlaceId,
                durationMinutes != null ? durationMinutes : 0,
                distanceFromPrevM != null ? distanceFromPrevM : 0);
        item.id = id;
        item.courseId = courseId;
        item.arrivalTime = arrivalTime;
        item.expectedCost = expectedCost;
        item.notes = notes;
        item.weatherWarning = weatherWarning;
        return item;
    }
}
