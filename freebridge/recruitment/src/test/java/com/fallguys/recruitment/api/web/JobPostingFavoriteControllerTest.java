package com.fallguys.recruitment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.support.TokenUserIdResolver;
import com.fallguys.recruitment.service.JobPostingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPostingFavoriteControllerTest {

    @Mock
    private JobPostingService jobPostingService;

    @Mock
    private TokenUserIdResolver tokenUserIdResolver;

    @InjectMocks
    private JobPostingFreelancerController controller;

    @Test
    @DisplayName("[TDD] 관심 공고 등록 API는 토큰 사용자 ID를 해석해 서비스에 전달하고 200을 반환한다")
    void addFavorite_callsServiceAndReturnsOk() {
        // given
        String authorization = "Bearer token";
        Long jobPostingId = 33L;
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(7L);

        // when
        ResponseEntity<ApiResponse<Void>> response = controller.addFavorite(authorization, jobPostingId);

        // then
        verify(tokenUserIdResolver, times(1)).resolveUserId(authorization);
        verify(jobPostingService, times(1)).addFavoriteJobPosting(7L, jobPostingId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
    }

    @Test
    @DisplayName("[TDD] 관심 공고 해제 API는 토큰 사용자 ID를 해석해 서비스에 전달하고 200을 반환한다")
    void removeFavorite_callsServiceAndReturnsOk() {
        // given
        String authorization = "Bearer token";
        Long jobPostingId = 44L;
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(8L);

        // when
        ResponseEntity<ApiResponse<Void>> response = controller.removeFavorite(authorization, jobPostingId);

        // then
        verify(tokenUserIdResolver, times(1)).resolveUserId(authorization);
        verify(jobPostingService, times(1)).removeFavoriteJobPosting(8L, jobPostingId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
    }
}
