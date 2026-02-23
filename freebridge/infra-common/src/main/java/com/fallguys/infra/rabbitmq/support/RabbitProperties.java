package com.fallguys.infra.rabbitmq.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fallguys.rabbitmq")
public class RabbitProperties {
    private String host;
    private int port;
    private String username;
    private String password;
}