package com.nolleo.onna.domain.external.congestion.dto;

import java.util.List;

public record CongestionResponse(
        String district,
        String rate,
        String baseYmd,
        List<CongestionAttractionInfo> attractions
) {
    public static CongestionResponse of(String district, CongestionInfo info) {
        return new CongestionResponse(
                district,
                info.rate(),
                info.baseYmd(),
                info.attractions()
        );
    }
}
