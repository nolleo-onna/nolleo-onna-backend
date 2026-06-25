package com.nolleo.onna.domain.generatedcourse.domain.model;

import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseInput;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseSummary;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseType;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.ShareInfo;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 코스 생성 Aggregate Root.
 * 자식 엔티티(GeneratedCourseItem)의 모든 변경은 반드시 이 Root를 통해서만 이루어진다.
 */
@Getter
public class GeneratedCourse {

    /** 내부 생성 코스 식별자 (PK) */
    private final Long id;

    /** 코스를 저장한 사용자 ID (FK) */
    private final Long userId;

    /** 재추천/복제 원본 코스 ID */
    private final Long parentCourseId;

    /** 같은 입력으로 생성된 형제 코스 묶음 UUID */
    private final UUID pairId;

    /** 생성 코스명 */
    private final String title;

    /** AI 생성 코스 추천 이유 설명 */
    private final String description;

    /** 사용자 입력 조건 묶음 (지역·예산·시간·동행·분위기) */
    private final CourseInput courseInput;

    /** 생성 모드 (예: "AI", "MANUAL") */
    private final String generationMode;

    /** 입력/생성 방식 (예: "FORM", "CHAT") */
    private final String generationMethod;

    /** AI 계산 코스 요약 수치 묶음 (총비용·총시간·절약액) */
    private CourseSummary courseSummary;

    /** 비교 기준 공식 여행코스 ID (TourAPI) */
    private final String comparedWithTravelCourseId;

    /** 추천 가중치 프로필 (JSONB) */
    private final String weightProfile;

    /** 코스 생성 시점 날씨/환경 스냅샷 (JSONB) */
    private final String weatherAtGen;

    /** 공개 공유 상태 묶음 (공개여부·토큰·조회수) */
    private ShareInfo shareInfo;

    /** 코스 타입 (ACTIVE / CULTURE / FOOD_TOUR) */
    private final CourseType courseType;

    /** 코스를 구성하는 방문 스팟 목록 */
    private final List<GeneratedCourseItem> items;

    /** 생성 시각 */
    private final OffsetDateTime createdAt;

    /** 생성 주체 */
    private final String createdBy;

    /** create() 전용 생성자 — 신규 코스 기본값 초기화 */
    private GeneratedCourse(Long userId, UUID pairId, CourseType courseType, String title,
                             String description, CourseInput courseInput,
                             String generationMode, String generationMethod, String createdBy) {
        this.id = null;
        this.userId = userId;
        this.parentCourseId = null;
        this.pairId = pairId;
        this.courseType = courseType;
        this.title = title;
        this.description = description;
        this.courseInput = courseInput;
        this.generationMode = generationMode;
        this.generationMethod = generationMethod;
        this.comparedWithTravelCourseId = null;
        this.weightProfile = null;
        this.weatherAtGen = null;
        this.shareInfo = ShareInfo.of(false, null, 0);
        this.items = new ArrayList<>();
        this.createdAt = OffsetDateTime.now();
        this.createdBy = createdBy;
    }

    /** restore() 전용 생성자 — DB 조회값 그대로 주입 */
    private GeneratedCourse(Long id, Long userId, Long parentCourseId, UUID pairId,
                             CourseType courseType, String title, String description,
                             CourseInput courseInput, String generationMode, String generationMethod,
                             CourseSummary courseSummary, String comparedWithTravelCourseId,
                             String weightProfile, String weatherAtGen, ShareInfo shareInfo,
                             List<GeneratedCourseItem> items, OffsetDateTime createdAt,
                             String createdBy) {
        this.id = id;
        this.userId = userId;
        this.parentCourseId = parentCourseId;
        this.pairId = pairId;
        this.courseType = courseType;
        this.title = title;
        this.description = description;
        this.courseInput = courseInput;
        this.generationMode = generationMode;
        this.generationMethod = generationMethod;
        this.courseSummary = courseSummary;
        this.comparedWithTravelCourseId = comparedWithTravelCourseId;
        this.weightProfile = weightProfile;
        this.weatherAtGen = weatherAtGen;
        this.shareInfo = shareInfo;
        this.items = new ArrayList<>(items);
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // ── 정적 팩토리 ──────────────────────────────────────────────────────────────

    /**
     * 코스 최초 생성.
     * 생성 시점에 확정되는 값만 받으며, 공개 여부는 항상 비공개로 초기화.
     */
    public static GeneratedCourse create(Long userId, UUID pairId, CourseType courseType,
                                          String title, String description,
                                          CourseInput courseInput, String generationMode,
                                          String generationMethod, String createdBy) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("코스 제목은 필수입니다.");

        return new GeneratedCourse(userId, pairId, courseType, title, description, courseInput,
                generationMode, generationMethod, createdBy);
    }

    /**
     * DB에서 읽어온 데이터로 도메인 객체를 재구성한다.
     * Repository 구현체에서 DB 조회 후 호출한다.
     */
    public static GeneratedCourse restore(Long id, Long userId, Long parentCourseId, UUID pairId,
                                               CourseType courseType, String title, String description,
                                               CourseInput courseInput, String generationMode,
                                               String generationMethod, CourseSummary courseSummary,
                                               String comparedWithTravelCourseId, String weightProfile,
                                               String weatherAtGen, ShareInfo shareInfo,
                                               List<GeneratedCourseItem> items,
                                               OffsetDateTime createdAt, String createdBy) {
        return new GeneratedCourse(id, userId, parentCourseId, pairId, courseType, title, description,
                courseInput, generationMode, generationMethod, courseSummary,
                comparedWithTravelCourseId, weightProfile, weatherAtGen, shareInfo,
                items, createdAt, createdBy);
    }

    /** 코스에 방문 스팟 추가 — Aggregate Root를 통해서만 아이템 생성 가능 */
    public void addItem(Long mapPlaceId, short durationMinutes, short distanceFromPrevM) {
        short nextSerial = (short) (items.size() + 1);
        items.add(new GeneratedCourseItem(nextSerial, mapPlaceId, durationMinutes, distanceFromPrevM));
    }

    /** @Getter가 생성하는 getItems()를 오버라이드 — 외부 수정 방지 */
    public List<GeneratedCourseItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
