package com.fallguys.recruitment.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecruitmentSwaggerConfig implements SwaggerConfigInterface {

    @Bean
    public GroupedOpenApi recruitmentGroupedOpenApi() {
        return createGroupedOpenApi(
                "recruitment", // 파라미터 1: group (Swagger 상단 Select 박스에 표시될 이름)
                "/api/v1/recruitment/**", // 파라미터 2: pathsToMatch (스캔할 컨트롤러들의 URL 패턴. 배열로 다중 입력 가능)
                "Recruitment API", // 파라미터 3: title (API 명세서 메인 제목)
                "채용 공고 등록 및 조회 도메인 API" // 파라미터 4: description (API 명세서 상세 설명)
        );
    }
}