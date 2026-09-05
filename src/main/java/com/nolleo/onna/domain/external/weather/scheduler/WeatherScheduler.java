package com.nolleo.onna.domain.external.weather.scheduler;

import com.nolleo.onna.domain.external.weather.WeatherRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 기상청 초단기실황 매 시간 15분마다 갱신.
 * 초단기실황은 정시마다 발표되며, 10분 후에 조회 가능.
 */
@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private final WeatherRefreshService weatherRefreshService;

    @Scheduled(cron = "0 15 * * * *")
    public void refreshAll() {
        weatherRefreshService.refresh();
    }
}
