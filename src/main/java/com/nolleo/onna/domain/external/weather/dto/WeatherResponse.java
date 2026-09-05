package com.nolleo.onna.domain.external.weather.dto;

public record WeatherResponse(
        String district,
        String tmp,
        String pty,
        String reh,
        String wsd,
        String rn1
) {
    public static WeatherResponse of(String district, WeatherInfo info) {
        return new WeatherResponse(
                district,
                info.tmp(),
                info.pty(),
                info.reh(),
                info.wsd(),
                info.rn1()
        );
    }
}
