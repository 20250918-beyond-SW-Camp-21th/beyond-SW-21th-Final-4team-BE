package com.fallguys.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable()) // REST API 이므로 CSRF 비활성화
                .formLogin(form -> form.disable()) // 기본 폼 로그인 비활성화
                .httpBasic(basic -> basic.disable()) // 기본 HTTP Basic 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용하지
                                                                                                               // 않음
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/contracts/**").authenticated()
                        .requestMatchers("/api/employer/**", "/api/v1/employer/**").hasRole("EMPLOYER")
                        .requestMatchers("/api/internal/payments/**").hasRole("EMPLOYER")
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
