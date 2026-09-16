package com.nolleo.onna.domain.course.application.dto;

import java.math.BigDecimal;

/**
 * Course 컨텍스트가 이해하는 방문 후보 스팟 정보.
 * Spot 컨텍스트의 도메인 모델(Spot/SpotCategory/GeoCoordinate)을 직접 참조하지 않기 위한
 * 경계 VO — SpotLookupPort의 어댑터가 Spot → SpotCandidate로 변환해서 넘겨준다.
 */
public record SpotCandidate(
        String contentId,
        String title,
        String firstImage,
        /** 카테고리 원본 코드 (FD/VE/NA/HS/EX/LS) — 음식점 여부 판단 등 내부 로직에 사용 */
        String categoryCode,
        /** 카테고리 한국어 라벨 — 응답 표시용 */
        String categoryLabel,
        /** 경도 (WGS84) */
        BigDecimal mapX,
        /** 위도 (WGS84) */
        BigDecimal mapY,
        /** 세부 분류 코드 (lcls_systm_2) — 후보 선택 시 같은 세부 분류(해수욕장만 3곳 등)가 몰리지 않게 하는 데 쓴다. 없으면 null */
        String subCategoryCode
) {
    /** 세부 분류 없이 만드는 편의 생성자 — 세부 분류가 필요 없는 조회·편집 경로와 테스트용 */
    public SpotCandidate(String contentId, String title, String firstImage, String categoryCode, String categoryLabel,
                         BigDecimal mapX, BigDecimal mapY) {
        this(contentId, title, firstImage, categoryCode, categoryLabel, mapX, mapY, null);
    }

    public boolean hasCoordinate() {
        return mapX != null && mapY != null;
    }

    public boolean isFood() {
        return "FD".equals(categoryCode);
    }
}
