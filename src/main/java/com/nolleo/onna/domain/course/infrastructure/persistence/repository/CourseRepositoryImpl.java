package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaceType;
import com.nolleo.onna.domain.course.domain.model.vo.CourseSort;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    public int incrementLikeCount(Long courseId) {
        jpaRepository.incrementLikeCount(courseId);
        return findLikeCount(courseId);
    }

    @Override
    public int decrementLikeCount(Long courseId) {
        jpaRepository.decrementLikeCount(courseId);
        return findLikeCount(courseId);
    }

    @Override
    public int findLikeCount(Long courseId) {
        Integer count = jpaRepository.findLikeCount(courseId);
        return count != null ? count : 0;
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

    /**
     * 2단계 조회 — (1) 최신순 코스 id courseLimit개 (2) 그 코스들의 SPOT 아이템 originalId.
     * JPQL 서브쿼리에는 LIMIT을 쓸 수 없어 id 페이지를 먼저 뗀다. 코스 본문·아이템 엔티티를 로딩하지 않고 id 스칼라만 읽는다.
     */
    @Override
    public Set<String> findRecentSpotContentIds(Long userId, int courseLimit) {
        if (courseLimit <= 0) return Set.of();
        List<Long> ids = jpaRepository.findRecentIdsByUserId(userId,
                PageRequest.of(0, courseLimit, toSort(CourseSort.LATEST)));
        if (ids.isEmpty()) return Set.of();
        return new HashSet<>(jpaRepository.findOriginalIdsByCourseIdIn(ids, CoursePlaceType.SPOT));
    }

    /**
     * 2단계 조회 — (1) 정렬된 id 페이지 (2) id IN 으로 아이템까지 fetch.
     * IN 조회는 순서를 보장하지 않으므로 (1)의 id 순서대로 다시 늘어놓는다.
     */
    @Override
    public List<Course> findPublic(CourseSort sort, int page, int size) {
        List<Long> ids = jpaRepository.findPublicIds(PageRequest.of(page, size, toSort(sort)));
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, CourseEntity> byId = jpaRepository.findWithItemsByIdIn(ids).stream()
                .collect(Collectors.toMap(CourseEntity::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(CourseEntity::toDomain)
                .toList();
    }

    /**
     * 정렬 기준 → JPA Sort. 뒷순위를 최신순 → id 내림차순으로 고정해 같은 값끼리의 순서가
     * 페이지마다 달라지지 않게 한다 (조회수·좋아요가 같은 코스가 많다).
     */
    static Sort toSort(CourseSort sort) {
        Sort latestThenId = Sort.by(Sort.Order.desc("createAudit.createdAt"), Sort.Order.desc("id"));
        return switch (sort) {
            case LATEST -> latestThenId;
            case LIKES -> Sort.by(Sort.Order.desc("likeCount")).and(latestThenId);
            case VIEWS -> Sort.by(Sort.Order.desc("viewCount")).and(latestThenId);
        };
    }
}
