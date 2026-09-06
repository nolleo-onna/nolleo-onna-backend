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
}
