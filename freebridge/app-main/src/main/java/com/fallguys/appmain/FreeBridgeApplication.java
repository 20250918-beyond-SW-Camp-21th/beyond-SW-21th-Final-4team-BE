package com.fallguys.appmain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fallguys")
@EntityScan(basePackages = "com.fallguys")
@EnableJpaRepositories(basePackages = "com.fallguys")
public class FreeBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FreeBridgeApplication.class, args);
    }
}