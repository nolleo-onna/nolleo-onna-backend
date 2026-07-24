package com.nolleo.onna.domain.course.domain.model;

import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CourseType;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 코스 생성 Aggregate Root. (generated_courses 테이블)
 * 자식 엔티티(CourseItem)의 모든 변경은 반드시 이 Root를 통해서만 이루어진다.
 */
@Getter
public class Course {

    /** 내부 생성 코스 식별자 (PK) */
    private final Long id;

    /** 코스를 저장한 사용자 ID (FK) */
    private final Long userId;

    /** 같은 요청으로 생성된 형제 코스 묶음 UUID */
    private final UUID pairId;

    /** 생성 방식 (AI / ALGORITHM) */
    private final GenerationMode generationMode;

    /** 코스 타입 — ALGORITHM 모드 전용, AI 모드는 null */
    private final CourseType courseType;

    /** 코스 제목 — AI: 질문 기반 생성 / ALGORITHM: 템플릿 */
    private String title;

    /** 코스 소개 문구 — AI: 완성된 코스에 대한 소개 생성 / ALGORITHM: 템플릿 */
    private String description;

    /** 코스 생성 의도 스냅샷 (JSONB) — 재현·디버깅용 */
    private final CourseIntent intent;

    /** 예상 총 비용 (원) — 음식점 포함 시 합산, 미포함 null */
    private Integer totalCost;

    /** 공개 공유 상태 묶음 (공개여부·토큰·조회수) */
    private ShareInfo shareInfo;

    /** 코스를 구성하는 방문 스팟 목록 */
    private final List<CourseItem> items;

    /** 생성 시각 */
    private final OffsetDateTime createdAt;

    /** 생성 주체 */
    private final String createdBy;

    private Course(Long id, Long userId, UUID pairId, GenerationMode generationMode,
                             CourseType courseType, String title, String description,
                             CourseIntent intent, Integer totalCost, ShareInfo shareInfo,
                             List<CourseItem> items, OffsetDateTime createdAt, String createdBy) {
        this.id = id;
        this.userId = userId;
        this.pairId = pairId;
        this.generationMode = generationMode;
        this.courseType = courseType;
        this.title = title;
        this.description = description;
        this.intent = intent;
        this.totalCost = totalCost;
        this.shareInfo = shareInfo;
        this.items = items;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // ── 정적 팩토리 ──────────────────────────────────────────────────────────

    /** AI(챗봇) 모드 코스 생성 — courseType 없음, title/description은 코스 구성 후 AI가 채움 */
    public static Course createByAi(Long userId, UUID pairId, CourseIntent intent, String createdBy) {
        validate(userId, intent);
        return new Course(null, userId, pairId, GenerationMode.AI, null,
                "생성 중", null, intent, null, ShareInfo.of(false, null, 0),
                new ArrayList<>(), OffsetDateTime.now(), createdBy);
    }

    /** ALGORITHM(폼) 모드 코스 생성 — courseType별 템플릿 title/description */
    public static Course createByAlgorithm(Long userId, UUID pairId, CourseType courseType,
                                                     CourseIntent intent, String createdBy) {
        validate(userId, intent);
        if (courseType == null) throw new IllegalArgumentException("ALGORITHM 모드는 courseType이 필수입니다.");
        return new Course(null, userId, pairId, GenerationMode.ALGORITHM, courseType,
                courseType.buildTitle(intent.startArea()),
                courseType.buildDescription(intent.companion()),
                intent, null, ShareInfo.of(false, null, 0),
                new ArrayList<>(), OffsetDateTime.now(), createdBy);
    }

    /** DB 조회값으로 도메인 객체 재구성 — Repository 구현체 전용 */
    public static Course restore(Long id, Long userId, UUID pairId,
                                           GenerationMode generationMode, CourseType courseType,
                                           String title, String description, CourseIntent intent,
                                           Integer totalCost, ShareInfo shareInfo,
                                           List<CourseItem> items,
                                           OffsetDateTime createdAt, String createdBy) {
        return new Course(id, userId, pairId, generationMode, courseType, title, description,
                intent, totalCost, shareInfo, new ArrayList<>(items), createdAt, createdBy);
    }

    private static void validate(Long userId, CourseIntent intent) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (intent == null || !intent.canGenerate()) {
            throw new IllegalArgumentException("startArea가 확정된 intent가 필요합니다.");
        }
    }

    // ── 도메인 로직 ──────────────────────────────────────────────────────────

    /** 코스에 방문 스팟 추가 — Aggregate Root를 통해서만 아이템 생성 가능 */
    public void addItem(String spotContentId, Integer expectedCost, short distanceFromPrevM) {
        short nextSerial = (short) (items.size() + 1);
        items.add(new CourseItem(nextSerial, spotContentId, expectedCost, distanceFromPrevM));
        this.totalCost = computeTotalCost();
    }

    /** AI가 생성한 제목·소개 문구 적용 — 코스 구성 완료 후 호출 */
    public void applyAiContent(String title, String description) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
        this.title = title;
        this.description = description;
    }

    /**
     * 예상 총 비용 계산 — 아이템 expectedCost(음식점만 존재) 합산.
     * 비용 데이터가 하나도 없으면 null (음식 미포함 코스).
     */
    private Integer computeTotalCost() {
        List<Integer> costs = items.stream()
                .map(CourseItem::getExpectedCost)
                .filter(Objects::nonNull)
                .toList();
        if (costs.isEmpty()) return null;
        return costs.stream().mapToInt(Integer::intValue).sum();
    }

    /** 코스 아이템이 참조하는 spotContentId 목록 — 장소 일괄 조회용 */
    public Set<String> getSpotContentIds() {
        return items.stream()
                .map(CourseItem::getSpotContentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** @Getter가 생성하는 getItems()를 오버라이드 — 외부 수정 방지 */
    public List<CourseItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
