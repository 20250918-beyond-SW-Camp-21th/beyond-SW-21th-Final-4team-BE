package com.fallguys.infra.s3.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fallguys.s3")
public class S3Properties {
    private String bucket;
    private String region;
    private Credentials credentials = new Credentials();
    private long presignedUrlExpirationMinutes = 60;

    @Getter @Setter
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }
}