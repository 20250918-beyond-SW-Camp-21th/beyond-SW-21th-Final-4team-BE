package com.fallguys.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {

    private String apiSecret;
    private String channelKey;
    private Subscription subscription = new Subscription();

    @Getter
    @Setter
    public static class Subscription {
        private Plan plan = new Plan();

        @Getter
        @Setter
        public static class Plan {
            private long pro = 29900L;
            private long prime = 59900L;
        }
    }
}
