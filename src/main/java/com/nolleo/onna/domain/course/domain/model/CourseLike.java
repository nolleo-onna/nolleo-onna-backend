package com.nolleo.onna.domain.course.domain.model;

import java.time.OffsetDateTime;

/**
 * 코스 좋아요 — 사용자와 코스의 관계 한 건 (generated_course_likes).
 * 사용자당 코스 1회는 DB UNIQUE (course_id, user_id)가 보장한다.
 * 좋아요 수는 Course.shareInfo.likeCount에 역정규화되며, 증감은 원자 UPDATE로만 이루어진다.
 */
public record CourseLike(Long id, Long courseId, Long userId, OffsetDateTime createdAt) {}
