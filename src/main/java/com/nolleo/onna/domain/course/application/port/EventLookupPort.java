package com.nolleo.onna.domain.course.application.port;

import com.nolleo.onna.domain.course.application.dto.EventCandidate;

import java.util.List;

/**
 * [아웃바운드 포트] Event 컨텍스트에서 기준점 후보 행사를 찾는다.
 *
 * Course 컨텍스트가 Event 컨텍스트의 도메인 모델·리포지토리에 직접 의존하지 않도록 하는 경계.
 * 구현(어댑터)은 infrastructure/event에 위치한다.
 */
public interface EventLookupPort {

    /**
     * 사용자가 말한 이름에 맞는, 아직 끝나지 않은 활성 행사를 "제목 정확 일치 → 시작일 이른 순"으로 최대 limit개 조회.
     * 공백·대소문자는 무시한다. 끝난 행사는 기준점으로 쓰지 않으므로 제외한다.
     * 호출자가 정확 일치·단일 결과면 확정하고, 여럿이면("부산국제" → 여러 행사) 사용자에게 고르게 한다.
     */
    List<EventCandidate> findUpcomingByTitle(String title, int limit);
}
