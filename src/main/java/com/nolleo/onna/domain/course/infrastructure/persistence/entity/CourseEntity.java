package com.nolleo.onna.domain.course.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.SoftDeleteAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.CourseType;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.course.infrastructure.persistence.converter.CourseIntentJson;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * generated_courses 테이블과 매핑되는 JPA 엔티티.
 * 도메인 객체(Course)와 분리되며, fromDomain/toDomain으로 변환한다.
 */
@Entity
@Table(name = "generated_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 코스를 저장한 사용자 ID (FK) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 같은 요청으로 생성된 형제 코스 묶음 UUID */
    @Column(name = "pair_id")
    private UUID pairId;

    /** 생성 방식 (AI / ALGORITHM) */
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false, length = 20)
    private GenerationMode generationMode;

    /** 코스 타입 — ALGORITHM 모드 전용, AI는 null */
    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", length = 20)
    private CourseType courseType;

    /** 코스 제목 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 코스 소개 문구 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 코스 생성 의도 스냅샷 (JSONB) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "intent", columnDefinition = "jsonb")
    private String intent;

    /** 예상 총 비용 (원) — 음식 미포함 시 null */
    @Column(name = "total_cost")
    private Integer totalCost;

    /** 코스 공개 여부 */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    /** 공유 URL 토큰 */
    @Column(name = "share_token", length = 64)
    private String shareToken;

    /** 공유 코스 누적 조회수 */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** 좋아요 수 — generated_course_likes 행 수의 역정규화 */
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    /**
     * 코스 방문 스팟 목록.
     * cascade=ALL + orphanRemoval: 코스와 아이템의 생명주기 동기화.
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("serialNum ASC")
    private List<CourseItemEntity> items = new ArrayList<>();

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    @Embedded
    private SoftDeleteAudit softDeleteAudit;

    /** 도메인 → 엔티티 변환. 자식 아이템도 함께 변환해 연관 관계를 설정한다. */
    public static CourseEntity fromDomain(Course course) {
        CourseEntity entity = new CourseEntity();
        entity.userId = course.getUserId();
        entity.pairId = course.getPairId();
        entity.generationMode = course.getGenerationMode();
        entity.courseType = course.getCourseType();
        entity.title = course.getTitle();
        entity.description = course.getDescription();
        entity.intent = CourseIntentJson.toJson(course.getIntent());
        entity.totalCost = course.getTotalCost();
        entity.isPublic = course.getShareInfo() != null && course.getShareInfo().isPublic();
        entity.shareToken = course.getShareInfo() != null ? course.getShareInfo().shareToken() : null;
        entity.viewCount = course.getShareInfo() != null ? course.getShareInfo().viewCount() : 0;
        entity.likeCount = course.getShareInfo() != null ? course.getShareInfo().likeCount() : 0;
        entity.createAudit = CreateAudit.now(course.getCreatedBy());
        entity.updateAudit = UpdateAudit.now();
        entity.softDeleteAudit = SoftDeleteAudit.active();
        course.getItems().stream()
                .map(item -> CourseItemEntity.fromDomain(item, entity))
                .forEach(entity.items::add);
        return entity;
    }

    /**
     * 자식 아이템을 전부 컬렉션에서 떼어낸다.
     * orphanRemoval=true라 flush 시 기존 행이 DELETE된다 — 새 아이템을 넣기 전에
     * 호출자가 flush를 한 번 끼워 넣어야 (course_id, serial_num) UNIQUE와 충돌하지 않는다.
     */
    public void clearItems() {
        items.clear();
    }

    /**
     * 재계산된 아이템 목록과 총비용을 반영한다 (코스 수정의 일괄 반영 지점).
     * clearItems() 이후에 호출하는 것을 전제로 하며, 순번은 도메인이 이미 1부터 재부여한 값이다.
     */
    public void applyItems(List<CourseItem> newItems, Integer totalCost, String updatedBy) {
        newItems.forEach(item -> items.add(CourseItemEntity.fromDomain(item, this)));
        this.totalCost = totalCost;
        if (this.updateAudit == null) this.updateAudit = UpdateAudit.now();
        this.updateAudit.touch(updatedBy);
    }

    /** 엔티티 → 도메인 재구성 */
    public Course toDomain() {
        List<CourseItem> domainItems = items.stream()
                .map(CourseItemEntity::toDomain)
                .toList();

        return Course.restore(
                id, userId, pairId, generationMode, courseType,
                title, description,
                CourseIntentJson.fromJson(intent),
                totalCost,
                ShareInfo.of(isPublic, shareToken, viewCount, likeCount),
                domainItems,
                createAudit != null ? createAudit.getCreatedAt() : null,
                createAudit != null ? createAudit.getCreatedBy() : null
        );
    }
}
