package com.fallguys.review.service;

import com.fallguys.review.api.dto.request.EmployerRejectionReasonCreateRequest;
import com.fallguys.review.api.dto.response.EmployerRejectionReasonResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployerRejectionReasonService {

    Long createEmployerRejectionReason(Long employerId, EmployerRejectionReasonCreateRequest request);

    Page<EmployerRejectionReasonResponseDTO> getEmployerRejectionReasons(Long employerId, String title, Pageable pageable);

    Page<EmployerRejectionReasonResponseDTO> getFreelancerRejectionReasons(Long freelancerId, String title, Pageable pageable);
}
