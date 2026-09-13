package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.application.port.UserLookupPort.UserProfile;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseSummaryResponse;
import com.nolleo.onna.domain.course.application.dto.response.PublicCourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseSort;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final SpotLookupPort spotLookupPort;
    private final UserLookupPort userLookupPort;

    /**
     * pairId로 묶인 코스(들)을 방문 스팟 상세 정보와 함께 조회한다.
     * 본인이 생성한 코스만 조회할 수 있다.
     */
    public List<CourseResponse> getByPairId(Long userId, UUID pairId) {
        List<Course> courses = courseRepository.findByPairId(pairId);
        if (courses.isEmpty()) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        courses.forEach(course -> course.validateOwnedBy(userId));
        return toResponses(courses);
    }

    /** 사용자가 생성한 코스 목록을 요약 정보(제목/소개/총비용/스팟 이름 목록)로 조회한다. 상세는 pairId로 별도 조회. */
    public List<CourseSummaryResponse> getByUserId(Long userId) {
        List<Course> courses = courseRepository.findByUserId(userId);
        Map<PlaceRef, SpotCandidate> spotByRef = loadSpots(courses);

        return courses.stream()
                .map(course -> CourseSummaryResponse.of(course, spotByRef))
                .toList();
    }

    /**
     * 공개 코스 목록 — 최신순 · 좋아요순 · 조회수순. 로그인 없이 볼 수 있어 소유자 검증이 없다.
     * 작성자 프로필은 id 묶음으로 한 번에 조회한다(N+1 방지). 탈퇴한 작성자는 null로 내려간다.
     * 조회수는 DB 반영값이다 — 단건 공유 조회와 달리 버퍼 대기분을 더하지 않는다(목록에서 코스마다 Redis를 읽지 않는다).
     */
    public List<PublicCourseResponse> getPublicCourses(CourseSort sort, int page, int size) {
        List<Course> courses = courseRepository.findPublic(sort, page, size);
        if (courses.isEmpty()) {
            return List.of();
        }
        Map<PlaceRef, SpotCandidate> spotByRef = loadSpots(courses);
        Set<Long> authorIds = courses.stream().map(Course::getUserId).collect(Collectors.toSet());
        Map<Long, UserProfile> authorById = userLookupPort.findByIds(authorIds);

        return courses.stream()
                .map(course -> PublicCourseResponse.of(course, spotByRef, authorById.get(course.getUserId())))
                .toList();
    }

    private List<CourseResponse> toResponses(List<Course> courses) {
        Map<PlaceRef, SpotCandidate> spotByRef = loadSpots(courses);
        return courses.stream()
                .map(course -> CourseResponse.of(course, spotByRef))
                .toList();
    }

    /**
     * 코스들이 참조하는 장소를 한 번에 조회한다.
     * 여러 코스가 같은 장소를 참조할 수 있으므로 중복을 제거한 뒤 조회한다.
     *
     * 현재 코스 생성은 SPOT만 담으므로 SpotLookupPort 하나로 충분하다.
     * FOOD 참조는 FOOD 허용 이슈에서 PlaceLookupPort로 확장하며, 그때 이 메서드만 바뀐다.
     * 결과 키를 PlaceRef로 두는 이유: SPOT "123"과 FOOD "123"이 같은 String 키로 충돌하는 것을 막는다.
     */
    private Map<PlaceRef, SpotCandidate> loadSpots(List<Course> courses) {
        Set<PlaceRef> refs = new LinkedHashSet<>();
        courses.forEach(course -> refs.addAll(course.getPlaceRefs()));

        List<String> spotContentIds = refs.stream()
                .filter(PlaceRef::isSpot)
                .map(PlaceRef::originalId)
                .toList();
        if (spotContentIds.isEmpty()) {
            return Map.of();
        }

        Map<PlaceRef, SpotCandidate> spotByRef = new HashMap<>();
        spotLookupPort.findByIds(spotContentIds)
                .forEach((contentId, spot) -> spotByRef.put(PlaceRef.spot(contentId), spot));
        return spotByRef;
    }
}
