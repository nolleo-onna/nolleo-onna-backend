package com.nolleo.onna.domain.course.domain.model.vo;

/**
 * 공개 코스 목록 정렬 기준. 값이 같을 때의 뒷순위(최신순 → id 내림차순)는 저장소 구현이 고정한다.
 */
public enum CourseSort {
    /** 생성 시각 최신순 */
    LATEST,
    /** 좋아요 수 내림차순 → 최신순 */
    LIKES,
    /** 조회수 내림차순 → 최신순 — 홈 인기 코스의 기본값 */
    VIEWS
}
