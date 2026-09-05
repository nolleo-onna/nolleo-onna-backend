package com.nolleo.onna.domain.external.congestion.scheduler;

import com.nolleo.onna.domain.external.congestion.CongestionRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TourAPI 관광지 집중률 매일 00:00 갱신.
 */
@Component
@RequiredArgsConstructor
public class CongestionScheduler {

    private final CongestionRefreshService congestionRefreshService;

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshAll() {
        congestionRefreshService.refresh();
    }
}
