package com.nolleo.onna.domain.course.infrastructure.persistence.entity;

import com.nolleo.onna.domain.course.domain.model.CourseLike;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * generated_course_likes 테이블 매핑 — PostLikeEntity / FavoriteEntity 와 같은 형태.
 * 쓰기는 CourseLikeJpaRepository의 INSERT ON CONFLICT / DELETE 벌크 쿼리로 하므로 이 엔티티로 save 하지 않는다.
 */
@Entity
@Table(
        name = "generated_course_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_generated_course_likes_course_user",
                columnNames = {"course_id", "user_id"}),
        indexes = @Index(name = "idx_generated_course_likes_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** generated_courses.id — 같은 컨텍스트라 DB FK 있음 (ON DELETE CASCADE) */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** mb_user_info.id — 컨텍스트 경계라 FK 없음 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public CourseLike toDomain() {
        return new CourseLike(id, courseId, userId, createdAt);
    }
}
