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
    public GroupedOpenApi mypageGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("mypage")
                .pathsToMatch(
                        "/api/employer/mypage/**",
                        "/api/freelancer/mypage/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new io.swagger.v3.oas.models.info.Info()
                                .title("MYPAGE API")
                                .description("MYPAGE Domain API")
                                .version("1.0.0")
                        )
                )
                .build();
    }
}