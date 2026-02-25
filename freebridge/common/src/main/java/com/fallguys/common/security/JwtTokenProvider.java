package com.fallguys.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    // 임시 하드코딩된 Secret Key입니다 (운영 시 환경 변수 분리하고 수정할 계획입니다)
    private static final String JWT_SECRET = "this-is-a-very-secure-secret-key-for-jwt-token-which-is-long-enough-for-hs256";
    // 토큰 만료 24시간
    private static final long JWT_EXPIRATION_MS = 1000L * 60 * 60 * 24;

    private final Key key;

    public JwtTokenProvider() {
        this.key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    public String generateToken(Long id, String email, String role, String name, String grade) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION_MS);

        return Jwts.builder()
                .subject(email)
                .claim("id", id)
                .claim("role", role)
                .claim("name", name)
                .claim("grade", grade != null ? grade : "")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith((javax.crypto.SecretKey) key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes())).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.warn("Invalid JWT Signature", ex);
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT Token", ex);
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT Token", ex);
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty.", ex);
        }
        return false;
    }
}
