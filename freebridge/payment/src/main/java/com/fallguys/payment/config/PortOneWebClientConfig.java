package com.fallguys.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class PortOneWebClientConfig {

    private static final String PORTONE_BASE_URL = "https://api.portone.io";

    private final PortOneProperties portOneProperties;

    @Bean(name = "portOneWebClient")
    public WebClient portOneWebClient() {
        return WebClient.builder()
                .baseUrl(PORTONE_BASE_URL)
                .defaultHeader("Authorization", "PortOne " + portOneProperties.getApiSecret())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
