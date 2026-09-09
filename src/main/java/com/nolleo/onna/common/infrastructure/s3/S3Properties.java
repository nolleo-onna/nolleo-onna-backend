package com.nolleo.onna.common.infrastructure.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
@Getter
@Setter
public class S3Properties {
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;

    // S3 호환 스토리지(GCS 등)의 API 엔드포인트. 미지정 시 AWS 기본 엔드포인트를 사용한다.
    private String endpoint;

    // 업로드된 객체의 공개 URL 접두사. 스토리지 교체나 CDN 도입 시 이 값만 바꾸면 된다.
    private String publicBaseUrl;
}
