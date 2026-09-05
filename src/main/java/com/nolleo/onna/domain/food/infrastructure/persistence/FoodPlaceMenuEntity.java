package com.nolleo.onna.domain.food.infrastructure.persistence;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.food.domain.model.FoodPlaceMenu;
import com.nolleo.onna.domain.food.domain.model.vo.Money;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** [JPA 엔티티] fd_food_place_menus 테이블 매핑. */
@Entity
@Table(name = "fd_food_place_menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FoodPlaceMenuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 음식 장소 ID */
    @Column(name = "food_place_id", nullable = false, updatable = false)
    private Long foodPlaceId;

    /** 메뉴/품목명 */
    @Column(name = "menu_name", nullable = false, length = 200)
    private String menuName;

    /** 현재 가격 */
    @Column(name = "price")
    private Integer price;

    /** 통화 코드 (ISO 4217) */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** 원천 파일 내 품목 노출 순서 */
    @Column(name = "display_order")
    private Short displayOrder;

    /** 현재 가격의 수집 원천 */
    @Column(name = "source", nullable = false, length = 40)
    private String source;

    /** 대표 메뉴 여부 */
    @Column(name = "is_representative", nullable = false)
    private boolean representative;

    /** 가격 최종 관측 시각 */
    @Column(name = "last_observed_at")
    private OffsetDateTime lastObservedAt;

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    /** 엔티티 → 도메인 모델 변환 */
    public FoodPlaceMenu toDomain() {
        return new FoodPlaceMenu(
                id, foodPlaceId, menuName,
                Money.of(price, currency),
                displayOrder, source, representative, lastObservedAt
        );
    }
}
