package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseItemsCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.Course.ItemDraft;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler.AssembledItem;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler.Waypoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 코스 수정 — 방문 스팟 목록 일괄 반영 (Full State Replacement).
 *
 * 순서:
 *   1. 코스 조회 + 소유자 검증
 *   2. 입력 검증 (개수 · 중복 · 장소 유형)
 *   3. 지역 중심 좌표 확정 (DistrictCenter — 1번 아이템 거리 기준)
 *   4. 장소 존재 · 좌표 검증 (SpotLookupPort)
 *   5. 전달된 순서 그대로 인접 거리 계산 (CourseAssembler.measure — 재배치 없음)
 *   6. FD 카테고리만 가격 조회
 *   7. 애그리거트 교체 (Course.replaceItems — 순번 재부여 · totalCost 재계산)
 *   8. 저장 1회
 *
 * 생성 파이프라인과 달리 외부 AI 호출이 없으므로 전체를 하나의 트랜잭션으로 묶는다.
 * 저장 1회 = 트랜잭션 1회라 "삭제는 됐는데 재배치는 실패한" 중간 상태가 남지 않는다.
 *
 * 이번 범위는 SPOT만 허용한다. FOOD는 PlaceLookupPort 도입 시 4·6단계만 바뀐다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseEditService {

    private final CourseRepository courseRepository;
    private final SpotLookupPort spotLookupPort;

    public CourseResponse updateItems(UpdateCourseItemsCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!Objects.equals(course.getUserId(), command.userId())) {
            throw new BusinessException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }

        List<PlaceRef> refs = command.items();
        validateRefs(refs);

        String startArea = course.getIntent() != null ? course.getIntent().startArea() : null;
        DistrictCenter center = DistrictCenter.of(startArea)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.UNKNOWN_START_AREA));

        Map<String, SpotCandidate> spotById = loadAndValidateSpots(refs);

        List<Waypoint> waypoints = refs.stream()
                .map(ref -> toWaypoint(spotById.get(ref.originalId())))
                .toList();
        List<AssembledItem> measured = CourseAssembler.measure(center.getLatitude(), center.getLongitude(), waypoints);

        List<String> foodContentIds = refs.stream()
                .map(ref -> spotById.get(ref.originalId()))
                .filter(SpotCandidate::isFood)
                .map(SpotCandidate::contentId)
                .toList();
        Map<String, Integer> priceByContentId = spotLookupPort.findFoodPrices(foodContentIds);

        List<ItemDraft> drafts = measured.stream()
                .map(item -> {
                    SpotCandidate spot = spotById.get(item.waypoint().refId());
                    Integer expectedCost = spot.isFood() ? priceByContentId.get(spot.contentId()) : null;
                    return new ItemDraft(PlaceRef.spot(spot.contentId()), expectedCost, item.distanceFromPrevM());
                })
                .toList();
        course.replaceItems(drafts);

        Course updated = courseRepository.update(course, String.valueOf(command.userId()));

        Map<PlaceRef, SpotCandidate> spotByRef = new HashMap<>();
        spotById.forEach((contentId, spot) -> spotByRef.put(PlaceRef.spot(contentId), spot));
        return CourseResponse.of(updated, spotByRef);
    }

    /** 클라이언트 입력 검증 — 도메인 불변식과 같은 규칙이지만 여기서는 400으로 매핑되는 BusinessException을 쓴다 */
    private static void validateRefs(List<PlaceRef> refs) {
        if (refs == null || refs.isEmpty()) {
            throw new BusinessException(CourseErrorCode.COURSE_ITEM_EMPTY);
        }
        if (refs.size() > Course.MAX_ITEMS) {
            throw new BusinessException(CourseErrorCode.COURSE_ITEM_LIMIT_EXCEEDED);
        }
        if (refs.stream().distinct().count() != refs.size()) {
            throw new BusinessException(CourseErrorCode.COURSE_ITEM_DUPLICATED);
        }
        if (refs.stream().anyMatch(ref -> !ref.isSpot())) {
            throw new BusinessException(CourseErrorCode.COURSE_PLACE_TYPE_NOT_SUPPORTED);
        }
    }

    /** 요청한 장소가 전부 존재하고 좌표를 가져야 한다 — 하나라도 빠지면 거리 계산이 불가능하므로 전체를 거부한다 */
    private Map<String, SpotCandidate> loadAndValidateSpots(List<PlaceRef> refs) {
        List<String> contentIds = refs.stream().map(PlaceRef::originalId).toList();
        Map<String, SpotCandidate> spotById = spotLookupPort.findByIds(contentIds);

        boolean allValid = contentIds.stream()
                .map(spotById::get)
                .allMatch(spot -> spot != null && spot.hasCoordinate());
        if (!allValid) {
            throw new BusinessException(CourseErrorCode.COURSE_PLACE_NOT_FOUND);
        }
        return spotById;
    }

    private static Waypoint toWaypoint(SpotCandidate spot) {
        return new Waypoint(spot.contentId(), spot.mapY().doubleValue(), spot.mapX().doubleValue());
    }
}
