package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseJpaRepository jpaRepository;

    @Override
    public Course save(Course course) {
        return jpaRepository.save(CourseEntity.fromDomain(course)).toDomain();
    }

    /**
     * 코스 편집 결과 반영 — 제목·소개 갱신 + 아이템 전량 교체.
     *
     * 삭제(flush)와 삽입을 명시적으로 분리한다. (course_id, serial_num)에 UNIQUE가 걸려 있어,
     * 순서만 바꾼 편집("1번을 3번으로")에서 새 행 INSERT가 기존 행 DELETE보다 먼저 나가면
     * 같은 순번이 잠시 두 건이 되어 제약에 걸린다.
     *
     * 같은 트랜잭션 안이라 findById는 1차 캐시의 관리 엔티티를 그대로 돌려준다.
     * 관리 엔티티이므로 save(merge)는 필요 없고 flush만으로 반영된다.
     */
    @Override
    public Course saveEdited(Course course, String actor) {
        CourseEntity entity = jpaRepository.findById(course.getId())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        entity.clearItems();
        jpaRepository.flush();

        entity.applyEdit(course, actor);
        jpaRepository.flush();
        return entity.toDomain();
    }

    /** toDomain()이 항상 아이템을 읽으므로 fetch join으로 한 번에 조회한다 (지연 로딩 추가 쿼리 제거) */
    @Override
    public Optional<Course> findById(Long id) {
        return jpaRepository.findWithItemsById(id).map(CourseEntity::toDomain);
    }

    /** 행 잠금 조회 — fetch join 없이 잠그고, toDomain()에서 아이템을 같은 트랜잭션 안에서 지연 로딩한다 */
    @Override
    public Optional<Course> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id).map(CourseEntity::toDomain);
    }

    @Override
    public Optional<Course> findPublicByShareToken(String shareToken) {
        return jpaRepository.findPublicByShareToken(shareToken).map(CourseEntity::toDomain);
    }

    /**
     * 공유 상태만 반영 — 관리 엔티티에 is_public · share_token을 쓰고 flush.
     * @DynamicUpdate라 바뀐 두 컬럼만 UPDATE되며, 벌크로 증감하는 view_count · like_count는 덮어쓰지 않는다.
     */
    @Override
    public Course saveShareState(Course course, String actor) {
        CourseEntity entity = jpaRepository.findById(course.getId())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
        entity.applyShareState(course.getShareInfo(), actor);
        jpaRepository.flush();
        return entity.toDomain();
    }

    @Override
    public void incrementViewCount(Long courseId) {
        jpaRepository.incrementViewCount(courseId);
    }

    /** 코스별로 원자 가산 UPDATE를 실행한다 — 호출자(CourseViewCountSink)의 트랜잭션 안에서 함께 커밋된다 */
    @Override
    public void addViewCounts(Map<Long, Long> deltaByCourseId) {
        deltaByCourseId.forEach((courseId, delta) ->
                jpaRepository.addViewCount(courseId, Math.toIntExact(delta)));
    }

    @Override
    public List<Course> findByPairId(UUID pairId) {
        return jpaRepository.findByPairId(pairId).stream()
                .map(CourseEntity::toDomain)
                .toList();
    }

    @Override
    public List<Course> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(CourseEntity::toDomain)
                .toList();
    }
}
