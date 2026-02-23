package com.fallguys.appmain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.fallguys.common.config, com.fallguys.config"})
public class FreeBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FreeBridgeApplication.class, args);
    }
}