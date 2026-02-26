package com.fallguys.infra.redis.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fallguys.redis")
public class RedisProperties {
    private String host;
    private int port;
    private String password;
}
