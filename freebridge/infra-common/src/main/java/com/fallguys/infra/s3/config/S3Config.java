package com.fallguys.infra.s3.config;

import com.fallguys.infra.s3.support.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    private final S3Properties properties;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getCredentials().getAccessKey(),
                                properties.getCredentials().getSecretKey()
                        )))
                .build();
    }
}