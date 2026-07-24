package com.nolleo.onna.domain.course.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
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

    /** sp_spots.content_id 참조 */
    @Column(name = "spot_content_id", length = 20)
    private String spotContentId;

    /** 예상 방문 비용 (원) — 음식점만 */
    @Column(name = "expected_cost")
    private Integer expectedCost;

    /** 이전 장소로부터 직선 거리 (미터) */
    @Column(name = "distance_from_prev_m")
    private Short distanceFromPrevM;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    /** 도메인 → 엔티티 변환. 부모 참조를 설정해 양방향 연관 관계를 완성한다. */
    public static CourseItemEntity fromDomain(CourseItem item,
                                                        CourseEntity courseEntity) {
        CourseItemEntity entity = new CourseItemEntity();
        entity.course = courseEntity;
        entity.serialNum = item.getSerialNum();
        entity.spotContentId = item.getSpotContentId();
        entity.expectedCost = item.getExpectedCost();
        entity.distanceFromPrevM = item.getDistanceFromPrevM();
        entity.createAudit = CreateAudit.now(courseEntity.getCreateAudit() != null
                ? courseEntity.getCreateAudit().getCreatedBy() : null);
        entity.updateAudit = UpdateAudit.now();
        return entity;
    }

    /** 엔티티 → 도메인 재구성 */
    public CourseItem toDomain() {
        return CourseItem.restore(
                id,
                course != null ? course.getId() : null,
                serialNum,
                spotContentId,
                expectedCost,
                distanceFromPrevM
        );
    }
}
