package com.fallguys.contract.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ContractSwaggerConfig implements SwaggerConfigInterface{

    @Bean
    public GroupedOpenApi contractGroupedOpenApi() {
        return createGroupedOpenApi(
                "contract", // 파라미터 1: group (Swagger 상단 Select 박스에 표시될 이름)
                "/api/contracts/**", // 파라미터 2: pathsToMatch (스캔할 컨트롤러들의 URL 패턴. 배열로 다중 입력 가능)
                "Contract API", // 파라미터 3: title (API 명세서 메인 제목)
                "계약 생성 및 확인 API" // 파라미터 4: description (API 명세서 상세 설명)
        );
    }
}
