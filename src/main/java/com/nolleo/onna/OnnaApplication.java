package com.nolleo.onna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // JwtProperties / CookieProperties 등 @ConfigurationProperties 빈 스캔
public class OnnaApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnnaApplication.class, args);
	}

}
