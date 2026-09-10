package com.nolleo.onna.domain.course.domain.model;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.CourseType;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler.AssembledItem;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler.Waypoint;
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

    /**
     * 코스 수정 시 전달하는 방문 장소의 최종 상태 — 방문 순서대로 담긴다.
     * 순번과 인접 거리는 담지 않는다. 좌표만 넘기면 애그리거트가 계산한다 (replaceItems).
     */
    public record VisitStop(PlaceRef placeRef, double latitude, double longitude, Integer expectedCost) {
        public VisitStop {
            Objects.requireNonNull(placeRef, "placeRef는 필수입니다.");
        }
    }

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
                "생성 중", null, intent, null, ShareInfo.initial(),
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
                intent, null, ShareInfo.initial(),
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
    public void addItem(PlaceRef placeRef, Integer expectedCost, Integer distanceFromPrevM) {
        short nextSerial = (short) (items.size() + 1);
        items.add(new CourseItem(nextSerial, placeRef, expectedCost, distanceFromPrevM));
        this.totalCost = computeTotalCost();
    }

    /**
     * 방문 스팟 목록을 최종 상태로 통째 교체한다 (Full State Replacement).
     *
     * 순번·인접 거리·totalCost는 모두 애그리거트가 계산한다. 호출자는 방문 순서와 좌표·비용만 넘기므로
     * "순번은 1부터 연속, 거리는 직전 지점 기준(첫 지점은 시작 지역 중심 기준)" 규칙이 외부 계산에 흔들리지 않는다.
     * 순서는 재배치하지 않는다 — 사용자가 편집으로 확정한 순서이기 때문이다 (CourseAssembler.measure).
     *
     * 검증(개수·중복·시작 지역)을 모두 마친 뒤에 기존 아이템을 비우므로, 실패하면 기존 상태가 그대로 보존된다.
     */
    public void replaceItems(List<VisitStop> stops) {
        List<PlaceRef> refs = stops == null ? null : stops.stream().map(VisitStop::placeRef).toList();
        new CoursePlaces(refs); // 개수·중복 불변식 — 규칙은 CoursePlaces 한 곳에만 있다
        DistrictCenter start = startPoint();

        List<Waypoint> waypoints = stops.stream()
                .map(stop -> new Waypoint(stop.placeRef().originalId(), stop.latitude(), stop.longitude()))
                .toList();
        List<AssembledItem> measured = CourseAssembler.measure(start.getLatitude(), start.getLongitude(), waypoints);

        items.clear();
        for (int i = 0; i < stops.size(); i++) {
            VisitStop stop = stops.get(i);
            addItem(stop.placeRef(), stop.expectedCost(), measured.get(i).distanceFromPrevM());
        }
    }

    /** 코스를 생성한 사용자인지 검증 — 조회·수정 공통 규칙 */
    public void validateOwnedBy(Long requesterId) {
        if (!Objects.equals(this.userId, requesterId)) {
            throw new BusinessException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    /** 코스의 출발 기준점 — 1번 아이템 거리를 재는 시작 지역 중심 좌표 */
    public DistrictCenter startPoint() {
        String startArea = intent != null ? intent.startArea() : null;
        return DistrictCenter.of(startArea)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.UNKNOWN_START_AREA));
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

    /** 코스 아이템이 참조하는 장소(PlaceRef) 목록 — 장소 일괄 조회용 */
    public Set<PlaceRef> getPlaceRefs() {
        return items.stream()
                .map(CourseItem::getPlaceRef)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** @Getter가 생성하는 getItems()를 오버라이드 — 외부 수정 방지 */
    public List<CourseItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
