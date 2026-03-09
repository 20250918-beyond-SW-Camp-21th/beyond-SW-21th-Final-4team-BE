package com.fallguys.chatting.api.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;

@Slf4j
@RestController
@RequestMapping("/api/chat/test/config")
@RequiredArgsConstructor
@Profile({ "local", "dev" })
public class ConfigTestController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MongoTemplate mongoTemplate;

    @Value("${fallguys.redis.host:unknown}")
    private String redisHost;

    @Value("${fallguys.redis.port:unknown}")
    private String redisPort;

    @Value("${spring.data.mongodb.host:unknown}")
    private String mongoHost;

    @GetMapping
    public ResponseEntity<Map<String, Object>> testConfigAndConnection() {
        Map<String, Object> response = new HashMap<>();

        // 1. 설정값 확인 (yml에서 주입된 값)
        Map<String, String> configValues = new HashMap<>();
        configValues.put("redis_host", redisHost);
        configValues.put("redis_port", redisPort);
        configValues.put("mongo_host", mongoHost);
        response.put("yml_properties", configValues);

        // 2. Redis 연결 테스트
        String testKey = "test_key_" + UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(testKey, "test_value");
            String value = (String) redisTemplate.opsForValue().get(testKey);
            response.put("redis_connection", "SUCCESS (test value: " + value + ")");
            redisTemplate.delete(testKey);
        } catch (Exception e) {
            log.error("Redis Connection Error", e);
            response.put("redis_connection", "FAIL (Internal test error)");
        }

        // 3. Mongo 연결 테스트
        try {
            org.bson.Document pingCommand = org.bson.Document.parse("{ ping: 1 }");
            org.bson.Document pingResult = mongoTemplate.executeCommand(pingCommand);
            boolean isMongoUp = false;
            if (pingResult != null && pingResult.get("ok") != null) {
                isMongoUp = ((Number) pingResult.get("ok")).doubleValue() == 1.0;
            }
            response.put("mongo_connection", isMongoUp ? "SUCCESS" : "FAIL");
        } catch (Exception e) {
            log.error("Mongo Connection Error", e);
            response.put("mongo_connection", "FAIL (Internal test error)");
        }

        return ResponseEntity.ok(response);
    }
}
