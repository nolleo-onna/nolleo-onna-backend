package com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.common.infrastructure.SoftDeleteAudit;
import com.nolleo.onna.common.infrastructure.UpdateAudit;
import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourse;
import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourseItem;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseInput;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseSummary;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseType;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.generatedcourse.infrastructure.persistence.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * cs_generated_courses 테이블과 매핑되는 JPA 엔티티.
 * 도메인 객체(GeneratedCourse)와 분리되며, fromDomain/toDomain으로 변환한다.
 */
@Entity
@Table(name = "cs_generated_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneratedCourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 코스를 저장한 사용자 ID (FK) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 재추천/복제 원본 코스 ID */
    @Column(name = "parent_course_id")
    private Long parentCourseId;

    /** 같은 입력으로 생성된 형제 코스 묶음 UUID */
    @Column(name = "pair_id")
    private UUID pairId;

    /** 코스 타입 (ACTIVE / CULTURE / FOOD_TOUR) */
    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", length = 20)
    private CourseType courseType;

    /** 생성 코스명 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** AI 생성 코스 추천 이유 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 사용자 입력 지역/시군구 */
    @Column(name = "input_signgu", length = 50)
    private String inputSigngu;

    /** 사용자 입력 예산 (원) */
    @Column(name = "input_budget")
    private Integer inputBudget;

    /** 사용자 입력 여행 시간 */
    @Column(name = "input_duration", length = 30)
    private String inputDuration;

    /** 사용자 입력 동행 유형 */
    @Column(name = "input_companion", length = 30)
    private String inputCompanion;

    /** 사용자 입력 분위기/태그 목록 (JSON 배열 문자열) */
    @Convert(converter = StringListConverter.class)
    @Column(name = "input_mood", columnDefinition = "text")
    private List<String> inputMood;

    /** 생성 모드 (예: "AI", "MANUAL") */
    @Column(name = "generation_mode", length = 20)
    private String generationMode;

    /** 입력/생성 방식 (예: "FORM", "CHAT") */
    @Column(name = "generation_method", length = 20)
    private String generationMethod;

    /** 예상 총 비용 (원) */
    @Column(name = "total_cost")
    private Integer totalCost;

    /** 예상 총 소요시간 (분) */
    @Column(name = "total_minutes")
    private Integer totalMinutes;

    /** 예상 절약 금액 (원) */
    @Column(name = "total_savings")
    private Integer totalSavings;

    /** 비교 기준 공식 여행코스 ID (TourAPI) */
    @Column(name = "compared_with_travel_course_id", length = 20)
    private String comparedWithTravelCourseId;

    /** 코스 공개 여부 */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    /** 공유 URL 토큰 */
    @Column(name = "share_token", length = 64)
    private String shareToken;

    /** 공유 코스 누적 조회수 */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /**
     * 코스 방문 스팟 목록 (내부 엔티티 컬렉션).
     * cascade=ALL: 코스 저장·삭제 시 아이템 생명주기 동기화.
     * orphanRemoval=true: 컬렉션에서 제거된 아이템은 DB에서도 삭제.
     * fetch=LAZY: 코스 단순 조회 시 불필요한 아이템 로딩 방지.
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("serialNum ASC")
    private List<GeneratedCourseItemEntity> items = new ArrayList<>();

    @Embedded
    private CreateAudit createAudit;

    @Embedded
    private UpdateAudit updateAudit;

    @Embedded
    private SoftDeleteAudit softDeleteAudit;

    /**
     * 도메인 객체에서 JPA 엔티티를 생성한다.
     * 코스의 자식(GeneratedCourseItem) 목록도 함께 변환하여 연관 관계를 설정한다.
     */
    public static GeneratedCourseEntity fromDomain(GeneratedCourse course) {
        GeneratedCourseEntity entity = new GeneratedCourseEntity();
        entity.id = course.getId();
        entity.userId = course.getUserId();
        entity.parentCourseId = course.getParentCourseId();
        entity.pairId = course.getPairId();
        entity.courseType = course.getCourseType();
        entity.title = course.getTitle();
        entity.description = course.getDescription();
        entity.inputSigngu = course.getCourseInput() != null ? course.getCourseInput().signgu() : null;
        entity.inputBudget = course.getCourseInput() != null ? course.getCourseInput().budget() : null;
        entity.inputDuration = course.getCourseInput() != null ? course.getCourseInput().duration() : null;
        entity.inputCompanion = course.getCourseInput() != null ? course.getCourseInput().companion() : null;
        entity.inputMood = course.getCourseInput() != null ? course.getCourseInput().mood() : null;
        entity.generationMode = course.getGenerationMode();
        entity.generationMethod = course.getGenerationMethod();
        entity.totalCost = course.getCourseSummary() != null ? course.getCourseSummary().totalCost() : null;
        entity.totalMinutes = course.getCourseSummary() != null ? course.getCourseSummary().totalMinutes() : null;
        entity.totalSavings = course.getCourseSummary() != null ? course.getCourseSummary().totalSavings() : null;
        entity.comparedWithTravelCourseId = course.getComparedWithTravelCourseId();
        entity.isPublic = course.getShareInfo() != null && course.getShareInfo().isPublic();
        entity.shareToken = course.getShareInfo() != null ? course.getShareInfo().shareToken() : null;
        entity.viewCount = course.getShareInfo() != null ? course.getShareInfo().viewCount() : 0;
        entity.createAudit = CreateAudit.now(course.getCreatedBy());
        entity.updateAudit = UpdateAudit.now();
        entity.softDeleteAudit = SoftDeleteAudit.active();
        // 자식 엔티티 변환 및 양방향 연관 관계 설정
        entity.items.clear();
        course.getItems().stream()
                .map(item -> GeneratedCourseItemEntity.fromDomain(item, entity))
                .forEach(entity.items::add);
        return entity;
    }

    /**
     * JPA 엔티티에서 도메인 객체를 재구성한다.
     * Repository 구현체에서 DB 조회 후 호출한다.
     */
    public GeneratedCourse toDomain() {
        List<GeneratedCourseItem> domainItems = items.stream()
                .map(GeneratedCourseItemEntity::toDomain)
                .toList();

        return GeneratedCourse.restore(
                id, userId, parentCourseId, pairId, courseType,
                title, description,
                CourseInput.of(inputSigngu, inputBudget, inputDuration, inputCompanion, inputMood),
                generationMode, generationMethod,
                CourseSummary.of(totalCost, totalMinutes, totalSavings),
                comparedWithTravelCourseId,
                ShareInfo.of(isPublic, shareToken, viewCount),
                domainItems,
                createAudit != null ? createAudit.getCreatedAt() : null,
                createAudit != null ? createAudit.getCreatedBy() : null
        );
    }
}
