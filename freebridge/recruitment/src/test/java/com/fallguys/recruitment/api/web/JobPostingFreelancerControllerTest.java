package com.fallguys.recruitment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.dto.response.AiRecommendationResponseDTO;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPostingFreelancerControllerTest {

    @Mock
    private JobPostingService jobPostingService;

    @Mock
    private TokenUserIdResolver tokenUserIdResolver;

    @InjectMocks
    private JobPostingFreelancerController controller;

    @Test
    @DisplayName("[TDD] 공고 검색 API는 liked/keyword를 전달하고 페이징 응답을 반환한다")
    void searchJobPostings_returnsPagedResponse() {
        // given
        String authorization = "Bearer token";
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(21L);
        when(jobPostingService.searchJobPostingsForFreelancer(21L, "java", true)).thenReturn(List.of(
                new FreelancerJobPostingSearchDTO(1L, "emp", "Backend", "desc", List.of("Java"), 100L, 3, 1, 0, true)
        ));

        // when
        ResponseEntity<ApiResponse<PagedResponseDTO<FreelancerJobPostingSearchDTO>>> response =
                controller.searchJobPostings(authorization, "java", true, 0, 10);

        // then
        verify(jobPostingService, times(1)).searchJobPostingsForFreelancer(21L, "java", true);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().data().content().size());
    }

    @Test
    @DisplayName("[TDD] 추천 공고 조회 API는 토큰 userId로 서비스 호출 후 결과 반환")
    void getMyJobRecommendations_callsServiceAndReturns() {
        // given
        String authorization = "Bearer token";
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(21L);
        when(jobPostingService.getRecommendedJobsForFreelancer(21L)).thenReturn(List.of(
                new AiRecommendationResponseDTO(1L, "추천공고", 0.93, null, null, null, null)
        ));

        // when
        ResponseEntity<ApiResponse<List<AiRecommendationResponseDTO>>> response =
                controller.getMyJobRecommendations(authorization);

        // then
        verify(jobPostingService, times(1)).getRecommendedJobsForFreelancer(21L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().data().size());
    }
}
