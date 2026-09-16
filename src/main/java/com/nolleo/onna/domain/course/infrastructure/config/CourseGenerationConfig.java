package com.nolleo.onna.domain.course.infrastructure.config;

import com.nolleo.onna.domain.course.domain.service.DiverseSpotSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

/**
 * 코스 생성 도메인 서비스의 빈 등록.
 * DiverseSpotSelector는 도메인 계층이라 Spring 애노테이션을 붙이지 않고 여기서 조립한다.
 * 난수원은 java.util.Random — 스레드 안전하며 보안 난수가 필요한 자리가 아니다.
 */
@Configuration
public class CourseGenerationConfig {

    @Bean
    public DiverseSpotSelector diverseSpotSelector() {
        return new DiverseSpotSelector(new Random());
    }
}
