package com.fallguys.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig implements SwaggerConfigInterface {

    @Bean
    public GroupedOpenApi userGroupedOpenApi() {
        return createGroupedOpenApi("user", "/api/users/**", "USER API", "USER Domain API");
    }

    @Bean
    public GroupedOpenApi employerMypageGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("employerMypage")
                .pathsToMatch(
                        "/api/employer/mypage/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new io.swagger.v3.oas.models.info.Info()
                                .title("EMPLOYER MYPAGE API")
                                .description("MYPAGE Domain API")
                                .version("1.0.0")
                        )
                )
                .build();
    }

    @Bean
    public GroupedOpenApi freelancerMypageGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("freelancerMypage")
                .pathsToMatch(
                        "/api/freelancer/mypage/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new io.swagger.v3.oas.models.info.Info()
                                .title("FREELANCER MYPAGE API")
                                .description("MYPAGE Domain API")
                                .version("1.0.0")
                        )
                )
                .build();
    }

    @Bean
    public GroupedOpenApi subscriptionGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("subscription")
                .pathsToMatch(
                        "/api/employer/subscription/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new io.swagger.v3.oas.models.info.Info()
                                .title("SUBSCRIPTION API")
                                .description("SUBSCRIPTION Domain API")
                                .version("1.0.0")
                        )
                )
                .build();
    }
}