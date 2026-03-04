package com.fallguys.matchs.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchsSwaggerConfig {

    @Bean
    public GroupedOpenApi matchsGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("matchs")
                .pathsToMatch(
                        "/api/employer/**",
                        "/api/freelancer/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new Info()
                                .title("Matchs API")
                                .description("지원 및 제안 매칭 도메인 API")
                                .version("1.0.0")
                        )
                )
                .build();
    }
}
