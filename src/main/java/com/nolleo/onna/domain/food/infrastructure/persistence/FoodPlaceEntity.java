package com.nolleo.onna.domain.food.infrastructure.persistence;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.food.domain.model.FoodPlace;
import com.nolleo.onna.domain.food.domain.model.vo.GeoCoordinate;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** [JPA 엔티티] fd_food_places 테이블 매핑. */
@Entity
@Table(name = "fd_food_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FoodPlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 업소명 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 원천 데이터의 업종명 */
    @Column(name = "business_category", length = 100)
    private String businessCategory;

    /** 서비스 표준 업종 분류 */
    @Column(name = "normalized_category", length = 50)
    private String normalizedCategory;

    /** 코스 식사 후보 여부 */
    @Column(name = "is_course_food_candidate", nullable = false)
    private boolean courseFoodCandidate;

    /** 업소 주소 */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** 업소 연락처 */
    @Column(name = "tel", length = 100)
    private String tel;

    /** 원천 데이터의 영업시간 문자열 */
    @Column(name = "business_hours_raw", length = 500)
    private String businessHoursRaw;

    /** 업소 소개 텍스트 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 대표 메뉴 문자열 (원천 raw 값) */
    @Column(name = "representative_menu", length = 200)
    private String representativeMenu;

    /** 배달 가능 여부 */
    @Column(name = "delivery_available")
    private Boolean deliveryAvailable;

    /** 주차 가능 여부 */
    @Column(name = "parking_available")
    private Boolean parkingAvailable;

    /** 원천 지역/구군명 */
    @Column(name = "source_region", length = 50)
    private String sourceRegion;

    /** 경도 (WGS84) */
    @Column(name = "map_x", precision = 10, scale = 7)
    private BigDecimal mapX;

    /** 위도 (WGS84) */
    @Column(name = "map_y", precision = 10, scale = 7)
    private BigDecimal mapY;

    /** 서비스 노출 활성 여부 */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** 비활성 전환 시각 */
    @Column(name = "inactive_since")
    private OffsetDateTime inactiveSince;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    /**
     * 메뉴 목록 — 같은 바운디드 컨텍스트 내 동일 애그리게이트이므로 @OneToMany 사용.
     * FK 제약조건 생성 방지: DB는 이미 생성된 상태이며 팀 합의에 따라 FK 없음.
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_place_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private List<FoodPlaceMenuEntity> menus = new ArrayList<>();

    /** 엔티티 → 도메인 모델 변환 (메뉴 포함) */
    public FoodPlace toDomain() {
        return new FoodPlace(
                id, name, businessCategory, normalizedCategory,
                courseFoodCandidate, address, tel, businessHoursRaw,
                description, representativeMenu, deliveryAvailable, parkingAvailable,
                sourceRegion, GeoCoordinate.of(mapX, mapY),
                active, inactiveSince,
                menus.stream().map(FoodPlaceMenuEntity::toDomain).toList()
        );
    }
}
