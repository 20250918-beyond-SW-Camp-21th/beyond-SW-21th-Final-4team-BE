package com.fallguys.payment.config;

import com.fallguys.common.config.SwaggerConfigInterface;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentSwaggerConfig implements SwaggerConfigInterface {

    @Bean
    public GroupedOpenApi paymentGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("payment")
                .pathsToMatch(
                        "/api/settlements/**",
                        "/api/wallets/**",
                        "/api/subscriptions/**",
                        "/api/internal/payments/**"
                )
                .addOpenApiCustomizer(openApi ->
                        openApi.setInfo(new io.swagger.v3.oas.models.info.Info()
                                .title("Payment API")
                                .description("정산, 지갑, 구독 결제 API")
                                .version("1.0.0")
                        )
                )
                .build();
    }
}
