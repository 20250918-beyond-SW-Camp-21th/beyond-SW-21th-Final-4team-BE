package com.fallguys.userlike.api.support;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLikeTokenUserIdResolverTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserLikeTokenUserIdResolver resolver;

    @Mock
    private Claims claims;

    @Test
    @DisplayName("[TDD] Authorization 헤더가 비어있으면 INVALID_INPUT_VALUE 예외를 던진다")
    void resolveUserId_blankHeader_throwsBusinessException() {
        // when
        BusinessException exception =
                assertThrows(BusinessException.class, () -> resolver.resolveUserId(""));

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(jwtTokenProvider, never()).validateToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("[TDD] Bearer 형식이 아니면 INVALID_INPUT_VALUE 예외를 던진다")
    void resolveUserId_invalidBearerPrefix_throwsBusinessException() {
        // when
        BusinessException exception =
                assertThrows(BusinessException.class, () -> resolver.resolveUserId("Token abc"));

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] 토큰 검증 실패 시 INVALID_INPUT_VALUE 예외를 던진다")
    void resolveUserId_invalidToken_throwsBusinessException() {
        // given
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        // when
        BusinessException exception =
                assertThrows(BusinessException.class, () -> resolver.resolveUserId("Bearer bad-token"));

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] subject가 숫자면 해당 값을 userId로 반환한다")
    void resolveUserId_numericSubject_returnsUserId() {
        // given
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("101");

        // when
        Long userId = resolver.resolveUserId("Bearer valid-token");

        // then
        assertEquals(101L, userId);
    }

    @Test
    @DisplayName("[TDD] subject가 비숫자면 id(Number) 클레임으로 fallback한다")
    void resolveUserId_nonNumericSubject_fallbackToNumberIdClaim() {
        // given
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("legacy-subject");
        when(claims.get("id")).thenReturn(202L);

        // when
        Long userId = resolver.resolveUserId("Bearer valid-token");

        // then
        assertEquals(202L, userId);
    }

    @Test
    @DisplayName("[TDD] subject가 비어있고 id(String) 클레임이 숫자면 파싱해 반환한다")
    void resolveUserId_blankSubject_parseStringIdClaim() {
        // given
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("");
        when(claims.get("id")).thenReturn("303");

        // when
        Long userId = resolver.resolveUserId("Bearer valid-token");

        // then
        assertEquals(303L, userId);
    }

    @Test
    @DisplayName("[TDD] subject/id 모두 userId로 해석 불가하면 INVALID_INPUT_VALUE 예외를 던진다")
    void resolveUserId_unresolvableClaims_throwsBusinessException() {
        // given
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("not-number");
        when(claims.get("id")).thenReturn("NaN");

        // when
        BusinessException exception =
                assertThrows(BusinessException.class, () -> resolver.resolveUserId("Bearer valid-token"));

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
