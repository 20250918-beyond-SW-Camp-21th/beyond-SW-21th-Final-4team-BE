package com.fallguys.userlike.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.userlike.api.support.UserLikeTokenUserIdResolver;
import com.fallguys.userlike.service.EmployerFreelancerFavoriteService;
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
class EmployerFreelancerFavoriteControllerTest {

    @Mock
    private EmployerFreelancerFavoriteService favoriteService;

    @Mock
    private UserLikeTokenUserIdResolver tokenUserIdResolver;

    @InjectMocks
    private EmployerFreelancerFavoriteController controller;

    @Test
    @DisplayName("[TDD] 즐겨찾기 등록 요청 시 userId를 해석해 서비스에 전달하고 200 OK를 반환한다")
    void addFavorite_callsServiceAndReturnsOk() {
        // given
        String authorization = "Bearer token";
        Long freelancerId = 55L;
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(11L);

        // when
        ResponseEntity<ApiResponse<Void>> response = controller.addFavorite(authorization, freelancerId);

        // then
        verify(tokenUserIdResolver, times(1)).resolveUserId(authorization);
        verify(favoriteService, times(1)).addFavorite(11L, freelancerId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
    }

    @Test
    @DisplayName("[TDD] 즐겨찾기 삭제 요청 시 userId를 해석해 서비스에 전달하고 200 OK를 반환한다")
    void removeFavorite_callsServiceAndReturnsOk() {
        // given
        String authorization = "Bearer token";
        Long freelancerId = 66L;
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(22L);

        // when
        ResponseEntity<ApiResponse<Void>> response = controller.removeFavorite(authorization, freelancerId);

        // then
        verify(tokenUserIdResolver, times(1)).resolveUserId(authorization);
        verify(favoriteService, times(1)).removeFavorite(22L, freelancerId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
    }
}
