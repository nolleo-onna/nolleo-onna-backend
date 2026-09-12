package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.application.port.UserLookupPort.UserProfile;
import com.nolleo.onna.common.application.service.ViewCountRecorder;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseVisibilityCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.SharedCourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.repository.CourseLikeRepository;
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
 *   공개 코스 조회 → 작성자 닉네임(UserLookupPort) → 스팟 상세 → 조회 기록(ViewCountRecorder — 버퍼에 누적,
 *   같은 viewer는 10분에 1회) → 표시 조회수 = DB + 대기분
 *   조회수는 ViewCountFlushService가 주기적으로 DB에 일괄 반영한다. 버퍼를 쓸 수 없으면 DB에 바로 +1 한다.
 *   조회 기록을 마지막에 두어 앞 단계가 실패하면 집계하지 않는다(Redis 집계는 트랜잭션 롤백으로 되돌려지지 않는다).
 *   Redis 호출이 트랜잭션(DB 커넥션 점유) 안에서 일어나므로 지연 상한은 spring.data.redis.timeout으로 제한한다.
 *   토큰 미존재 · 비공개 · 삭제를 구분하지 않고 모두 COURSE_NOT_FOUND — 존재 여부를 노출하지 않는다.
 *   버퍼 장애 시 DB 증가(@Modifying)로 대체하므로 readOnly 트랜잭션이 아니어야 한다.
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
    private final ViewCountRecorder viewCountRecorder;
    private final CourseLikeRepository courseLikeRepository;

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

    /**
     * @param viewerKey    조회자 식별 키 — 같은 viewer의 재조회는 일정 시간 동안 조회수에 한 번만 집계된다
     * @param viewerUserId 로그인한 조회자의 회원 id, 비로그인이면 null — 있을 때만 "내가 좋아요 했는지"를 조회한다
     */
    public SharedCourseResponse getShared(String shareToken, String viewerKey, Long viewerUserId) {
        Course course = courseRepository.findPublicByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        UserProfile author = userLookupPort.findById(course.getUserId()).orElse(null);
        Map<PlaceRef, SpotCandidate> spotByRef = loadSpots(course);
        boolean likedByMe = viewerUserId != null && courseLikeRepository.exists(course.getId(), viewerUserId);

        Long courseId = course.getId();
        long pendingViews = viewCountRecorder.record(CourseViewCountSink.TARGET_TYPE, courseId, viewerKey,
                () -> courseRepository.incrementViewCount(courseId));
        course.applyPendingViews(pendingViews);

        return SharedCourseResponse.of(course, spotByRef, author, likedByMe);
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
