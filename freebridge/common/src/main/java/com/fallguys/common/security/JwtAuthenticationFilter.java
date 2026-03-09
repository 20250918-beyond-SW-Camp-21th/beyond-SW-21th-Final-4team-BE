package com.fallguys.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("Blocked request with blacklisted JWT");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
                    return;
                }

                var claims = jwtTokenProvider.getClaimsFromToken(jwt);
                Long userId = resolveUserId(claims);
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                String roleName = role != null ? (role.startsWith("ROLE_") ? role : "ROLE_" + role) : null;

                java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = roleName != null
                        ? Collections.singletonList(new SimpleGrantedAuthority(roleName))
                        : Collections.emptyList();

                // CustomUserDetails를 생성하여 인증 객체의 Principal로 설정
                CustomUserDetails userDetails = CustomUserDetails.builder()
                        .id(userId)
                        .email(email)
                        .name(claims.get("name", String.class))
                        .role(role)
                        .grade(claims.get("grade", String.class))
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Long resolveUserId(io.jsonwebtoken.Claims claims) {
        String subject = claims.getSubject();
        if (StringUtils.hasText(subject)) {
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException ignored) {
                // fallback for legacy token format
            }
        }

        Object idClaim = claims.get("id");
        if (idClaim instanceof Number number) {
            return number.longValue();
        }
        if (idClaim instanceof String str && StringUtils.hasText(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        return null;
    }
}
