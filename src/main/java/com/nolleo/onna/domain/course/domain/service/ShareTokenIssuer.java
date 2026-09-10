package com.nolleo.onna.domain.course.domain.service;

/**
 * 코스 공유 토큰 발급자 — 도메인이 "토큰이 필요할 때만" 발급을 요청하는 함수형 인터페이스.
 *
 * ShareInfo.publish가 토큰이 아직 없을 때만 호출한다. 기본 구현은 ShareTokenGenerator::generate이고,
 * 테스트에서는 람다로 고정 토큰이나 "호출되면 안 된다"를 표현한다.
 */
@FunctionalInterface
public interface ShareTokenIssuer {

    /** 추측 불가능한 새 공유 토큰을 발급한다 */
    String issue();
}
