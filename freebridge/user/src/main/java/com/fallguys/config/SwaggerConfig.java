package com.fallguys.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig implements SwaggerConfigInterface {

    @Bean
    public GroupedOpenApi userGroupedOpenApi() {
        return createGroupedOpenApi("user", "/user/**", "USER API", "USER Domain API");
    }

    @Bean
    public GroupedOpenApi mypageGroupedOpenApi() {
        return createGroupedOpenApi("mypage", "/api/mypage/**", "MYPAGE API", "MYPAGE Domain API");
    }
}