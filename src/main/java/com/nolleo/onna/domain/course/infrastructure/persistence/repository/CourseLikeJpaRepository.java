package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.infrastructure.persistence.entity.CourseLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseLikeJpaRepository extends JpaRepository<CourseLikeEntity, Long> {

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);

    /**
     * 없을 때만 삽입 — 이미 있으면 0행. UNIQUE 위반 예외를 내지 않으므로 트랜잭션이 abort되지 않는다.
     * 같은 사용자의 동시 토글이 겹쳐도 정확히 한 요청만 1을 받는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO generated_course_likes (course_id, user_id, created_at)
            VALUES (:courseId, :userId, NOW())
            ON CONFLICT (course_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("courseId") Long courseId, @Param("userId") Long userId);

    /** 있을 때만 삭제 — 없으면 0행. 동시 취소가 겹쳐도 한 요청만 1을 받는다 */
    @Modifying
    @Query("DELETE FROM CourseLikeEntity l WHERE l.courseId = :courseId AND l.userId = :userId")
    int deleteByCourseIdAndUserId(@Param("courseId") Long courseId, @Param("userId") Long userId);
}
