package com.nolleo.onna.domain.course.domain.model.vo;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;

import java.util.HashSet;
import java.util.List;

/**
 * 코스에 담기는 방문 장소 목록 (방문 순서대로).
 *
 * "최소 1개 · 최대 MAX_ITEMS개 · 같은 장소 중복 금지" 규칙의 단일 출처다.
 * 응용 계층은 스팟 조회 전에 이 VO를 먼저 만들어 잘못된 요청을 400으로 빠르게 거르고,
 * 애그리거트(Course.replaceItems)도 같은 VO로 불변식을 지킨다 — 규칙 구현은 이 클래스 한 곳뿐이다.
 * 중복 판단은 PlaceRef 값 동등성(type + originalId) 기준이다.
 */
public record CoursePlaces(List<PlaceRef> refs) {

    /** 한 코스에 담을 수 있는 방문 장소 최대 개수 — 요청 DTO의 @Size와 에러 메시지도 이 값을 참조한다 */
    public static final int MAX_ITEMS = 15;

    public CoursePlaces {
        if (refs == null || refs.isEmpty()) {
            throw new BusinessException(CourseErrorCode.COURSE_ITEM_EMPTY);
        }
        // 방어적 복사. null 원소는 여기서 NPE — 요청 DTO가 먼저 막으므로 이 경로에 오면 프로그래밍 오류다
        refs = List.copyOf(refs);
        if (refs.size() > MAX_ITEMS) {
            throw new BusinessException(CourseErrorCode.COURSE_ITEM_LIMIT_EXCEEDED);
        }
        if (new HashSet<>(refs).size() != refs.size()) {
            throw new BusinessException(CourseErrorCode.COURSE_ITEM_DUPLICATED);
        }
    }

    /** 모든 장소가 관광공사 스팟(SPOT)인지 */
    public boolean allSpots() {
        return refs.stream().allMatch(PlaceRef::isSpot);
    }
}
