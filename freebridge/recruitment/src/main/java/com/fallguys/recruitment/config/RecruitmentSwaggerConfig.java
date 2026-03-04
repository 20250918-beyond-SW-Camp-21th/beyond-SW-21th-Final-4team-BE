package com.fallguys.recruitment.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecruitmentSwaggerConfig implements SwaggerConfigInterface {

    @Bean
    public GroupedOpenApi recruitmentGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("recruitment")
                .pathsToMatch(
                        "/api/employer/**",
                        "/api/freelancer/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new Info()
                                .title("Recruitment API")
                                .description("채용 공고 등록 및 조회 도메인 API")
                                .version("1.0.0")
                        )
                )
                .build();
    }
}
