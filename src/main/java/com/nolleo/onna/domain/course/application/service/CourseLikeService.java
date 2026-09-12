package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.response.CourseLikeToggleResponse;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.repository.CourseLikeRepository;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 좋아요 토글 — 공유 링크로 열람 중인 공개 코스에 사용자당 1회.
 *
 * 대상은 shareToken으로 받는다. 공개 코스 조회(findPublicByShareToken)가 is_public = true 만 허용하므로
 * "공개 코스에만 좋아요" 규칙이 조회 조건에서 강제되고, 공개 영역에 코스 id를 노출하지 않는다.
 * 소유자 본인도 가능하다 — 1인 1회는 UNIQUE가 보장하므로 따로 막을 이유가 없다.
 *
 * 동시성 — 락을 쓰지 않는다:
 *   같은 사용자의 동시 토글은 add/remove가 영향 행 수로 판정해 한쪽만 true가 되고, 그쪽만 like_count를 증감한다.
 *   다른 사용자들의 동시 좋아요는 like_count ± 1 원자 UPDATE가 행 락을 잡는다.
 *   exists 판정과 add/remove 사이에 상태가 바뀌어도 결과는 "바뀐 뒤 상태"로 수렴하며 카운터가 어긋나지 않는다.
 *
 * 저장은 DB — 조회수(Redis 버퍼)와 달리 좋아요는 정확해야 하는 사용자 관계라 게시글 좋아요·찜과 같은 저장소를 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseLikeService {

    private final CourseRepository courseRepository;
    private final CourseLikeRepository courseLikeRepository;

    public CourseLikeToggleResponse toggle(String shareToken, Long userId) {
        Course course = courseRepository.findPublicByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
        Long courseId = course.getId();

        if (courseLikeRepository.exists(courseId, userId)) {
            boolean removed = courseLikeRepository.remove(courseId, userId);
            int likeCount = removed
                    ? courseRepository.decrementLikeCount(courseId)
                    : courseRepository.findLikeCount(courseId);   // 동시 취소가 먼저 지운 경우 — 카운터는 그쪽이 이미 내렸다
            return new CourseLikeToggleResponse(false, likeCount);
        }

        boolean added = courseLikeRepository.add(courseId, userId);
        int likeCount = added
                ? courseRepository.incrementLikeCount(courseId)
                : courseRepository.findLikeCount(courseId);       // 동시 좋아요가 먼저 넣은 경우 — 카운터는 그쪽이 이미 올렸다
        return new CourseLikeToggleResponse(true, likeCount);
    }
}
