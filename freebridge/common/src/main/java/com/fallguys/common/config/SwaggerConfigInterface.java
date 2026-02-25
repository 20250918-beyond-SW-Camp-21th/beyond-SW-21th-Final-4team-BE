package com.fallguys.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;

public interface SwaggerConfigInterface {
    default GroupedOpenApi createGroupedOpenApi(String group, String path, String title, String description) {
        return GroupedOpenApi.builder()
                .group(group)
                .pathsToMatch(path)
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new Info()
                                .title(title)
                                .description(description)
                                .version("1.0.0")
                        )
                )
                .build();
    }

    default OpenAPI createOpenAPI(String title, String description) {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version("1.0.0"));
    }
}
