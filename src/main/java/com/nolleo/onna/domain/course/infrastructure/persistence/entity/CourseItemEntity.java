package com.nolleo.onna.domain.course.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaceType;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * generated_course_items 테이블과 매핑되는 JPA 엔티티.
 * CourseEntity의 자식으로, 코스에 포함된 개별 방문 스팟을 나타낸다.
 */
@Entity
@Table(name = "generated_course_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 코스 — 연관관계 주인(FK 보유 측). LAZY: 아이템 단독 조회 시 코스 로딩 방지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    /** 코스 내 방문 순서 (1부터 시작) */
    @Column(name = "serial_num", nullable = false)
    private Short serialNum;

    /** 참조 장소의 원본 테이블 구분 — SPOT | FOOD */
    @Enumerated(EnumType.STRING)
    @Column(name = "place_type", nullable = false, length = 10)
    private CoursePlaceType placeType;

    /** 원본 식별자 — SPOT → sp_spots.content_id, FOOD → fd_food_places.id */
    @Column(name = "original_id", nullable = false, length = 50)
    private String originalId;

    /** 예상 방문 비용 (원) — 음식점만 */
    @Column(name = "expected_cost")
    private Integer expectedCost;

    /** 이전 장소로부터 직선 거리 (미터) — 부산 최장 구간이 SMALLINT 상한을 넘어 INTEGER */
    @Column(name = "distance_from_prev_m")
    private Integer distanceFromPrevM;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    /** 도메인 → 엔티티 변환 (코스 생성 시) — 아이템 행의 생성 주체는 코스 생성 주체와 같다. */
    public static CourseItemEntity fromDomain(CourseItem item, CourseEntity courseEntity) {
        String courseCreatedBy = courseEntity.getCreateAudit() != null
                ? courseEntity.getCreateAudit().getCreatedBy() : null;
        return fromDomain(item, courseEntity, courseCreatedBy);
    }

    /**
     * 도메인 → 엔티티 변환. 부모 참조를 설정해 양방향 연관 관계를 완성한다.
     * createdBy는 이 행을 실제로 만든 주체다 — 코스 수정으로 다시 삽입되는 행은 편집한 사용자가 된다.
     */
    public static CourseItemEntity fromDomain(CourseItem item, CourseEntity courseEntity, String createdBy) {
        CourseItemEntity entity = new CourseItemEntity();
        entity.course = courseEntity;
        entity.serialNum = item.getSerialNum();
        entity.placeType = item.getPlaceRef().type();
        entity.originalId = item.getPlaceRef().originalId();
        entity.expectedCost = item.getExpectedCost();
        entity.distanceFromPrevM = item.getDistanceFromPrevM();
        entity.createAudit = CreateAudit.now(createdBy);
        entity.updateAudit = UpdateAudit.now();
        return entity;
    }

    /** 엔티티 → 도메인 재구성 */
    public CourseItem toDomain() {
        return CourseItem.restore(
                id,
                course != null ? course.getId() : null,
                serialNum,
                new PlaceRef(placeType, originalId),
                expectedCost,
                distanceFromPrevM
        );
    }
}
