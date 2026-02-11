package com.fallguys.infra.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.Executors;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}
