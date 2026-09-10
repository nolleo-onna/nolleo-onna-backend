package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.application.port.UserLookupPort.UserProfile;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseVisibilityCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.SharedCourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.service.ShareTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 코스 공유 — 공개 전환과 공유 링크 열람.
 *
 * 공개 전환 (updateVisibility):
 *   행 잠금 조회(findByIdForUpdate) → 소유자 검증(Course.validateOwnedBy) → publish/unpublish → 공유 상태만 저장(saveShareState)
 *   토큰은 최초 공개 때만 발급되고 이후 유지된다 (Course.publish가 ShareTokenIssuer를 필요할 때만 호출).
 *   행 잠금으로 같은 코스에 대한 동시 전환을 직렬화한다. 첫 공개가 동시에 들어와도 두 번째 요청은 첫 요청의 커밋을
 *   기다렸다가 이미 발급된 토큰을 읽으므로, 토큰이 두 번 발급되어 먼저 받은 링크가 404가 되는 일이 없다.
 *
 * 공유 링크 열람 (getShared):
 *   공개 코스 조회 → 조회수 원자 증가(incrementViewCount) → 작성자 닉네임(UserLookupPort) → 스팟 상세 병합
 *   토큰 미존재 · 비공개 · 삭제를 구분하지 않고 모두 COURSE_NOT_FOUND — 존재 여부를 노출하지 않는다.
 *   조회수 증가가 @Modifying 쿼리라 readOnly 트랜잭션이 아니어야 한다.
 *
 * 작성자 정보는 common의 UserLookupPort로 가져온다 — Course 컨텍스트가 User 도메인을 직접 참조하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseShareService {

    private final CourseRepository courseRepository;
    private final SpotLookupPort spotLookupPort;
    private final UserLookupPort userLookupPort;

    public CourseResponse updateVisibility(UpdateCourseVisibilityCommand command) {
        Course course = courseRepository.findByIdForUpdate(command.courseId())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
        course.validateOwnedBy(command.userId());

        if (command.isPublic()) {
            course.publish(ShareTokenGenerator::generate);
        } else {
            course.unpublish();
        }

        Course saved = courseRepository.saveShareState(course, String.valueOf(command.userId()));
        return CourseResponse.of(saved, loadSpots(saved));
    }

    public SharedCourseResponse getShared(String shareToken) {
        Course course = courseRepository.findPublicByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        courseRepository.incrementViewCount(course.getId());
        course.markViewed();

        UserProfile author = userLookupPort.findById(course.getUserId()).orElse(null);
        return SharedCourseResponse.of(course, loadSpots(course), author);
    }

    /** 코스가 참조하는 스팟 상세를 한 번에 조회한다. 현재 코스는 SPOT만 담으므로 SpotLookupPort로 충분하다. */
    private Map<PlaceRef, SpotCandidate> loadSpots(Course course) {
        List<String> spotContentIds = course.getPlaceRefs().stream()
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
