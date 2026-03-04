package com.fallguys.review.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewSwaggerConfig implements SwaggerConfigInterface {

    @Bean
    public GroupedOpenApi reviewGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("review")
                .pathsToMatch(
                        "/api/employer/reviews/**",
                        "/api/employer/projects/*/reviews",
                        "/api/freelancer/reviews/**",
                        "/api/freelancer/projects/*/reviews",
                        "/api/reviews/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new Info()
                                .title("Review API")
                                .description("리뷰 작성 및 조회 도메인 API")
                                .version("1.0.0")
                        )
                )
                .build();
    }
}
