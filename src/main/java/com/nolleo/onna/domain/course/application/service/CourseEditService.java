package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.Course.VisitStop;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 코스 수정 — 제목 · 소개 · 방문 스팟 목록 일괄 반영 (Full State Replacement).
 *
 * 순서:
 *   1. 코스 조회 + 소유자 검증 (Course.validateOwnedBy)
 *   2. 입력 검증 — 개수·중복은 도메인 VO(CoursePlaces), SPOT 한정은 이번 릴리스 정책이라 여기서 거른다
 *   3. 시작 지역 해석 (Course.startPoint) — 스팟 조회 전에 끊기 위해 먼저 확인한다
 *   4. 활성 스팟 존재 · 좌표 검증 (SpotLookupPort.findActiveByIds)
 *   5. FD 카테고리만 가격 조회
 *   6. 애그리거트 편집 (Course.edit — 제목·소개 검증, 순번 · 인접 거리 · totalCost 계산을 애그리거트가 수행)
 *   7. 저장 1회
 *
 * 규칙 판단은 도메인에 위임하고, 이 서비스는 조회 · 변환 · 저장 순서만 조율한다.
 * 생성 파이프라인과 달리 외부 AI 호출이 없으므로 전체를 하나의 트랜잭션으로 묶는다.
 * 저장 1회 = 트랜잭션 1회라 "제목은 바뀌었는데 스팟 교체는 실패한" 중간 상태가 남지 않는다.
 *
 * 이번 범위는 SPOT만 허용한다. FOOD는 PlaceLookupPort 도입 시 2·4·5단계가 바뀐다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseEditService {

    private final CourseRepository courseRepository;
    private final SpotLookupPort spotLookupPort;

    public CourseResponse updateCourse(UpdateCourseCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
        course.validateOwnedBy(command.userId());

        CoursePlaces places = new CoursePlaces(command.items());
        if (!places.allSpots()) {
            throw new BusinessException(CourseErrorCode.COURSE_PLACE_TYPE_NOT_SUPPORTED);
        }
        course.startPoint(); // 해석 불가면 UNKNOWN_START_AREA — 스팟 조회 전에 끊는다

        Map<String, SpotCandidate> spotById = loadActiveSpots(places.refs());
        Map<String, Integer> priceByContentId = spotLookupPort.findFoodPrices(foodContentIds(places.refs(), spotById));

        List<VisitStop> stops = places.refs().stream()
                .map(ref -> toVisitStop(ref, spotById.get(ref.originalId()), priceByContentId))
                .toList();
        course.edit(command.title(), command.description(), stops);

        Course updated = courseRepository.saveEdited(course, String.valueOf(command.userId()));

        Map<PlaceRef, SpotCandidate> spotByRef = new HashMap<>();
        places.refs().forEach(ref -> spotByRef.put(ref, spotById.get(ref.originalId())));
        return CourseResponse.of(updated, spotByRef);
    }

    /** 요청한 장소가 전부 활성 상태로 존재하고 좌표를 가져야 한다 — 하나라도 빠지면 거리 계산이 불가능하므로 전체를 거부한다 */
    private Map<String, SpotCandidate> loadActiveSpots(List<PlaceRef> refs) {
        List<String> contentIds = refs.stream().map(PlaceRef::originalId).toList();
        Map<String, SpotCandidate> spotById = spotLookupPort.findActiveByIds(contentIds);

        boolean allValid = contentIds.stream()
                .map(spotById::get)
                .allMatch(spot -> spot != null && spot.hasCoordinate());
        if (!allValid) {
            throw new BusinessException(CourseErrorCode.COURSE_PLACE_NOT_FOUND);
        }
        return spotById;
    }

    /** 요청 순서를 유지한 음식점 content_id 목록 — 가격 일괄 조회용 */
    private static List<String> foodContentIds(List<PlaceRef> refs, Map<String, SpotCandidate> spotById) {
        return refs.stream()
                .map(ref -> spotById.get(ref.originalId()))
                .filter(SpotCandidate::isFood)
                .map(SpotCandidate::contentId)
                .toList();
    }

    /** 요청의 PlaceRef를 그대로 유지한다 — contentId 문자열로 다시 조립하면 SPOT/FOOD 구분이 사라진다 */
    private static VisitStop toVisitStop(PlaceRef ref, SpotCandidate spot, Map<String, Integer> priceByContentId) {
        Integer expectedCost = spot.isFood() ? priceByContentId.get(spot.contentId()) : null;
        return new VisitStop(ref, spot.mapY().doubleValue(), spot.mapX().doubleValue(), expectedCost);
    }
}
