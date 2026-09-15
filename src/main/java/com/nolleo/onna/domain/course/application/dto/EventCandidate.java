package com.nolleo.onna.domain.course.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Course 컨텍스트가 이해하는 행사 정보 — 기준점(CourseAnchor) 후보.
 * Event 컨텍스트의 도메인 모델을 직접 참조하지 않기 위한 경계 VO. EventLookupPort의 어댑터가 변환해서 넘겨준다.
 */
public record EventCandidate(
        String contentId,
        String title,
        /** 경도 (WGS84) */
        BigDecimal mapX,
        /** 위도 (WGS84) */
        BigDecimal mapY,
        LocalDate startDate,
        LocalDate endDate,
        String place
) {
    public boolean hasCoordinate() {
        return mapX != null && mapY != null;
    }
}
