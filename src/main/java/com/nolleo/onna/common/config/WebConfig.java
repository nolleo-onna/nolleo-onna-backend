package com.nolleo.onna.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1",
                url -> (url.isAnnotationPresent(RestController.class) ||
                        url.isAnnotationPresent(Controller.class)) &&
                       url.getPackageName().startsWith("com.nolleo.onna"));
    }
}