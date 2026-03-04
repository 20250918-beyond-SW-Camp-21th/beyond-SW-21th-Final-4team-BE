package com.fallguys.recruitment.api.support;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenUserIdResolverTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private TokenUserIdResolver resolver;

    @Mock
    private Claims claims;

    @Test
    @DisplayName("[TDD] Authorization 형식이 잘못되면 INVALID_INPUT_VALUE 예외")
    void resolveUserId_invalidHeader_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () -> resolver.resolveUserId("Token abc"));
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] subject가 숫자면 userId로 반환")
    void resolveUserId_subjectNumber_returnsId() {
        when(jwtTokenProvider.validateToken("ok")).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken("ok")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("101");

        Long userId = resolver.resolveUserId("Bearer ok");

        assertEquals(101L, userId);
    }

    @Test
    @DisplayName("[TDD] subject가 비숫자면 id(Number) claim으로 fallback")
    void resolveUserId_fallbackToIdClaim() {
        when(jwtTokenProvider.validateToken("ok")).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken("ok")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("legacy");
        when(claims.get("id")).thenReturn(202L);

        Long userId = resolver.resolveUserId("Bearer ok");

        assertEquals(202L, userId);
    }
}
