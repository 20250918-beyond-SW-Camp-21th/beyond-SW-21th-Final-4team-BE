package com.fallguys.chatting.security;

import com.fallguys.common.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatTokenProviderImpl implements ChatTokenProvider {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public String getUserIdFromToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.");
        }

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        // 클레임에서 id (Long)을 가져올 때 Integer/Long 형 변환 에러 방지용으로 Number로 받음
        Number idObj = claims.get("id", Number.class);
        if (idObj == null) {
            throw new IllegalArgumentException("토큰에 'id' 클레임이 존재하지 않습니다.");
        }
        Long id = idObj.longValue();

        String role = claims.get("role", String.class);
        if (role == null) {
            throw new IllegalArgumentException("토큰에 'role' 클레임이 존재하지 않습니다.");
        }

        if ("ROLE_EMPLOYER".equals(role)) {
            return "e" + id;
        } else if ("ROLE_FREELANCER".equals(role)) {
            return "f" + id;
        } else if ("ROLE_ADMIN".equals(role)) {
            return "a" + id;
        } else {
            throw new IllegalArgumentException("알 수 없는 권한입니다: " + role);
        }
    }
}
