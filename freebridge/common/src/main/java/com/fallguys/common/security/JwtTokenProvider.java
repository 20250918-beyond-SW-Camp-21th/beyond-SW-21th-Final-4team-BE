package com.fallguys.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final JwtParser jwtParser;
    private final long jwtExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${security.jwt.secret:}") String jwtSecret,
            @Value("${security.jwt.expiration-ms:3600000}") long jwtExpirationMs,
            @Value("${security.jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Missing required config: security.jwt.secret (env: JWT_SECRET)");
        }

        try {
            this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid JWT secret: must be sufficiently long for HS256", e);
        }

        this.jwtParser = Jwts.parser().verifyWith(this.key).build();
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateToken(Long id, String email, String role, String name, String grade) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(id)) // 토큰의 주체(Subject)를 변하지 않는 식별자인 ID로 설정
                .claim("email", email) // 이메일은 별도의 클레임으로 저장
                .claim("role", role)
                .claim("name", name)
                .claim("grade", grade != null ? grade : "") // 회원 등급
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long id) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(String.valueOf(id))
                .id(jti)
                .claim("token_type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return jwtParser.parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
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

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
