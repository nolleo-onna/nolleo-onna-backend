package com.nolleo.onna.domain.course.domain.repository;

import com.nolleo.onna.domain.course.domain.model.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {

    Course save(Course course);

    /**
     * 코스 편집 결과를 반영한다 — 제목 · 소개 · 방문 스팟 목록 · totalCost (코스 수정 = 최종 상태 일괄 반영).
     * 기존 아이템 행은 전부 삭제되고 새 순번으로 다시 삽입된다.
     * 공유 상태 · 조회수 · 좋아요 수는 반영하지 않는다 — 각자 전용 경로로 변경한다.
     *
     * @param actor 변경 주체. updated_by와, 새로 삽입되는 아이템 행의 created_by에 기록된다
     */
    Course saveEdited(Course course, String actor);

    Optional<Course> findById(Long id);

    /**
     * 행 잠금(SELECT ... FOR UPDATE)을 걸고 조회한다 — 읽고 판단해 쓰는 경로(공유 상태 전환)용.
     * 같은 코스에 대한 동시 요청을 직렬화해, 첫 공개 요청이 동시에 들어와도 토큰이 한 번만 발급되게 한다.
     * 트랜잭션 안에서 호출해야 하며, 잠금은 커밋·롤백 시 풀린다.
     */
    Optional<Course> findByIdForUpdate(Long id);

    /** 공유 토큰으로 공개 코스 단건 조회 — 비공개·삭제된 코스는 제외한다 (존재 여부를 노출하지 않는다) */
    Optional<Course> findPublicByShareToken(String shareToken);

    /**
     * 공유 상태(is_public · share_token)만 반영한다. 제목·아이템·카운터는 건드리지 않는다.
     *
     * @param actor 변경 주체 — updated_by에 기록
     */
    Course saveShareState(Course course, String actor);

    /** 공유 링크 조회수 +1 — 동시 조회에서 유실되지 않도록 원자 UPDATE로 증가시킨다 */
    void incrementViewCount(Long courseId);

    List<Course> findByPairId(UUID pairId);

    List<Course> findByUserId(Long userId);
}
