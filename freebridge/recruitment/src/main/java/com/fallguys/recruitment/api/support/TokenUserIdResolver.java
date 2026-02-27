package com.fallguys.recruitment.api.support;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("recruitmentTokenUserIdResolver")
@RequiredArgsConstructor
public class TokenUserIdResolver {

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

        Number userIdValue = jwtTokenProvider.getClaimsFromToken(token).get("id", Number.class);
        if (userIdValue == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return userIdValue.longValue();
    }
}
