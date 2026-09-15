package com.nolleo.onna.domain.course.application.dto;

import com.nolleo.onna.domain.course.domain.model.vo.BudgetTier;

import java.util.List;

/**
 * 폼 기반 코스 생성 명령 — 지역 · 예산 등급 · 꼭 포함할 장소명 · 축제명.
 * 검증(필수·개수·길이)은 요청 DTO가 끝냈고, 이름 → 스팟/행사 매칭은 서비스가 한다.
 */
public record CreateCourseCommand(
        Long userId,
        String startArea,
        BudgetTier budget,
        List<String> includeSpots,
        String festival
) {
    public CreateCourseCommand {
        if (budget == null) budget = BudgetTier.UNLIMITED;
        includeSpots = includeSpots == null ? List.of() : List.copyOf(includeSpots);
        if (festival != null && festival.isBlank()) festival = null;
    }
}
