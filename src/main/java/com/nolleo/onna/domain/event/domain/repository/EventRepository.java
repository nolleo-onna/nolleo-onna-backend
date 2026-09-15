package com.nolleo.onna.domain.event.domain.repository;

import com.nolleo.onna.domain.event.domain.model.Event;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [도메인 포트] Event 저장소 인터페이스.
 * 구현체: infrastructure/persistence/EventRepositoryImpl
 */
public interface EventRepository {

    List<Event> findAllActive();

    Optional<Event> findByContentId(String contentId);

    /**
     * 활성 행사 중 제목이 키워드를 포함하고 today 기준 아직 끝나지 않은(종료일 없음 또는 today 이후) 행사를
     * "정확히 일치 → 시작일 이른 순"으로 최대 limit개 조회. 공백·대소문자를 무시하고 비교한다.
     * 코스 기준점("X 근처") 매칭용.
     */
    List<Event> findActiveByTitleNotEnded(String title, LocalDate today, int limit);
}
