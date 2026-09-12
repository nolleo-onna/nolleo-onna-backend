package com.nolleo.onna.domain.course.domain.repository;

/**
 * 코스 좋아요 저장소 포트.
 *
 * add / remove는 "실제로 바뀌었는가"를 돌려준다. 같은 사용자의 동시 토글이 겹쳐도
 * INSERT ON CONFLICT DO NOTHING · 조건부 DELETE의 영향 행 수로 판정하므로 예외 없이 한 쪽만 true가 되고,
 * 호출자는 true일 때만 like_count를 증감한다. 락이 필요 없는 이유다.
 */
public interface CourseLikeRepository {

    /** 좋아요 추가. 이미 있으면 아무것도 하지 않고 false */
    boolean add(Long courseId, Long userId);

    /** 좋아요 삭제. 없으면 아무것도 하지 않고 false */
    boolean remove(Long courseId, Long userId);

    boolean exists(Long courseId, Long userId);
}
