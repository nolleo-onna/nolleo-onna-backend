package com.nolleo.onna.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 외부 HTTP 호출(Gemini · OpenAI · 날씨 · 혼잡도) 공용 RestTemplate.
 *
 * connect/read 타임아웃을 반드시 건다 — 기본 팩토리는 타임아웃이 없어서 외부 API가 응답을 멈추면
 * 요청 스레드가 무기한 점유된다. 값은 app.http.* 로 외부화하고, Redis 타임아웃(spring.data.redis.timeout)과
 * 같은 원칙(응답 없는 의존성 때문에 스레드·커넥션을 붙잡지 않는다)으로 둔다.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${app.http.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.http.read-timeout-ms:15000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .forEach(c -> ((StringHttpMessageConverter) c).setDefaultCharset(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
