package com.nolleo.onna.domain.external.weather.dto;

/**
 * Redis에 캐싱되는 날씨 실황 정보. (기상청 초단기실황 기준)
 *   tmp - 기온 (°C, T1H)
 *   pty - 강수형태 (0:없음 1:비 2:비/눈 3:눈, PTY)
 *   reh - 습도 (%, REH)
 *   wsd - 풍속 (m/s, WSD)
 *   rn1 - 1시간 강수량 (mm, RN1)
 */
public record WeatherInfo(
        String tmp,
        String pty,
        String reh,
        String wsd,
        String rn1
) {
    public static WeatherInfo empty() {
        return new WeatherInfo("-", "0", "-", "-", "0");
    }
}
