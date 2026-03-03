package com.fallguys.userlike.api.support;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("userLikeTokenUserIdResolver")
@RequiredArgsConstructor
public class UserLikeTokenUserIdResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public Long resolveUserId(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        var claims = jwtTokenProvider.getClaimsFromToken(token);

        String subject = claims.getSubject();
        if (StringUtils.hasText(subject)) {
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException ignored) {
                // fallback to legacy id claim
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

        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
