package com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourseItem;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.WeatherWarning;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * cs_generated_course_items 테이블과 매핑되는 JPA 엔티티.
 * GeneratedCourseEntity의 자식으로, 코스에 포함된 개별 방문 스팟을 나타낸다.
 */
@Entity
@Table(name = "cs_generated_course_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneratedCourseItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 코스 — 양방향 관계에서 연관관계 주인(FK 보유 측).
     * LAZY 로딩: 아이템 단독 조회 시 코스 전체를 불필요하게 로딩하지 않도록 설정.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private GeneratedCourseEntity course;

    /** 코스 내 방문 순서 (1부터 시작) */
    @Column(name = "serial_num", nullable = false)
    private Short serialNum;

    /** mp_map_places.id 참조 — 이를 통해 SPOT/FOOD 원본 테이블까지 탐색 */
    @Column(name = "map_place_id")
    private Long mapPlaceId;

    /** 예상 도착 시각 */
    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    /** 예상 체류 시간 (분) */
    @Column(name = "duration_minutes")
    private Short durationMinutes;

    /** 예상 방문 비용 (원) */
    @Column(name = "expected_cost")
    private Integer expectedCost;

    /** 이전 장소로부터 직선 거리 (미터) */
    @Column(name = "distance_from_prev_m")
    private Short distanceFromPrevM;

    /** 타임라인에 표시할 메모 */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** 날씨 위험 경고 여부 */
    @Column(name = "has_weather_warning", nullable = false)
    private boolean hasWeatherWarning;

    /** 사용자 노출 날씨 경고 메시지 */
    @Column(name = "warning_message", columnDefinition = "TEXT")
    private String warningMessage;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    // ── 정적 팩토리 ──────────────────────────────────────────────────────────────

    /**
     * 도메인 객체에서 JPA 엔티티를 생성한다.
     * 부모 엔티티 참조를 함께 설정하여 양방향 연관 관계를 완성한다.
     */
    public static GeneratedCourseItemEntity fromDomain(GeneratedCourseItem item,
                                                        GeneratedCourseEntity courseEntity) {
        GeneratedCourseItemEntity entity = new GeneratedCourseItemEntity();
        entity.course = courseEntity;
        entity.serialNum = item.getSerialNum();
        entity.mapPlaceId = item.getMapPlaceId();
        entity.arrivalTime = item.getArrivalTime();
        entity.durationMinutes = item.getDurationMinutes();
        entity.expectedCost = item.getExpectedCost();
        entity.distanceFromPrevM = item.getDistanceFromPrevM();
        entity.notes = item.getNotes();
        entity.hasWeatherWarning = item.getWeatherWarning() != null && item.getWeatherWarning().hasWarning();
        entity.warningMessage = item.getWeatherWarning() != null ? item.getWeatherWarning().message() : null;
        entity.createAudit = CreateAudit.now(courseEntity.getCreateAudit() != null
                ? courseEntity.getCreateAudit().getCreatedBy() : null);
        entity.updateAudit = UpdateAudit.now();
        return entity;
    }

    // ── 도메인 변환 ──────────────────────────────────────────────────────────────

    /**
     * JPA 엔티티에서 도메인 객체를 재구성한다.
     * Repository 구현체에서 DB 조회 후 호출한다.
     */
    public GeneratedCourseItem toDomain() {
        return GeneratedCourseItem.restore(
                id,
                course != null ? course.getId() : null,
                serialNum,
                mapPlaceId,
                arrivalTime,
                durationMinutes,
                expectedCost,
                distanceFromPrevM,
                notes,
                hasWeatherWarning ? WeatherWarning.of(warningMessage) : WeatherWarning.none()
        );
    }
}
