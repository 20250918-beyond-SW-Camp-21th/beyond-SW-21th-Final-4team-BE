package com.fallguys.userlike.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserLikeSwaggerConfig implements SwaggerConfigInterface {

    @Bean
    public GroupedOpenApi userLikeGroupedOpenApi() {
        return createGroupedOpenApi(
                "userlike",
                "/api/employer/freelancers/**",
                "UserLike API",
                "고용주 즐겨찾기(프리랜서) 도메인 API"
        );
    }
}
