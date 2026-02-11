package com.fallguys.common.port;

public interface MessagePublisher {
    void publish(String exchange, String routingKey, Object message);
}
