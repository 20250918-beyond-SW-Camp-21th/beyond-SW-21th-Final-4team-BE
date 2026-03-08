package com.fallguys.recruitment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.MatchedFreelancerResponseDTO;
import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;
import com.fallguys.recruitment.api.support.TokenUserIdResolver;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.ProjectStatus;
import com.fallguys.recruitment.service.JobPostingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPostingEmployerControllerTest {

    @Mock
    private JobPostingService jobPostingService;

    @Mock
    private TokenUserIdResolver tokenUserIdResolver;

    @InjectMocks
    private JobPostingEmployerController controller;

    @Test
    @DisplayName("[TDD] 채용 공고 생성 API는 토큰 userId를 해석해 서비스에 전달하고 200 반환")
    void createJobPosting_callsServiceAndReturnsOk() {
        // given
        String authorization = "Bearer token";
        JobPostingCreateDTO body = new JobPostingCreateDTO("Backend", "desc", List.of("Java"), 1000L, 3, 2);
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(10L);

        // when
        ResponseEntity<ApiResponse<Void>> response = controller.createJobPosting(authorization, body);

        // then
        verify(jobPostingService, times(1)).createJobPosting(body, 10L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
    }

    @Test
    @DisplayName("[TDD] 내 공고 조회 API는 PagingUtils를 통해 page/size를 보정해 응답한다")
    void getMyJobPostings_appliesPagingSafety() {
        // given
        String authorization = "Bearer token";
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(10L);
        when(jobPostingService.getJobPostings(10L)).thenReturn(List.of(
                new JobPostingSearchDTO(1L, "emp", "t1", "d1", List.of("Java"), 100L, 1, 1, 0, JobPostingStatus.OPEN),
                new JobPostingSearchDTO(2L, "emp", "t2", "d2", List.of("Spring"), 200L, 2, 2, 0, JobPostingStatus.IN_PROGRESS)
        ));

        // when
        ResponseEntity<ApiResponse<PagedResponseDTO<JobPostingSearchDTO>>> response =
                controller.getMyJobPostings(authorization, -3, 0);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        PagedResponseDTO<JobPostingSearchDTO> paged = response.getBody().data();
        assertEquals(0, paged.page());
        assertEquals(1, paged.size());
        assertEquals(2L, paged.totalElements());
        assertEquals(2, paged.totalPages());
        assertEquals(1, paged.content().size());
    }

    @Test
    @DisplayName("[TDD] 프로젝트 매칭 프리랜서 목록 조회 API는 안전한 PageRequest를 사용해 페이징 응답한다")
    void getMatchedFreelancers_appliesPagingSafety() {
        String authorization = "Bearer token";
        Long projectId = 55L;
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(10L);
        when(jobPostingService.getMatchedFreelancers(projectId, 10L, PageRequest.of(0, 1))).thenReturn(new PageImpl<>(List.of(
                new MatchedFreelancerResponseDTO(
                        55L,
                        21L,
                        "freelancer",
                        "[Java, Spring]",
                        "백엔드 5년",
                        "ACTIVE",
                        ProjectStatus.IN_PROGRESS,
                        LocalDateTime.of(2026, 3, 8, 18, 0)
                )
        ), PageRequest.of(0, 1), 3));

        ResponseEntity<ApiResponse<PagedResponseDTO<MatchedFreelancerResponseDTO>>> response =
                controller.getMatchedFreelancers(authorization, projectId, -1, 0);

        verify(jobPostingService, times(1)).getMatchedFreelancers(projectId, 10L, PageRequest.of(0, 1));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());

        PagedResponseDTO<MatchedFreelancerResponseDTO> paged = response.getBody().data();
        assertEquals(0, paged.page());
        assertEquals(1, paged.size());
        assertEquals(3L, paged.totalElements());
        assertEquals(3, paged.totalPages());
        assertEquals(1, paged.content().size());
    }
}
