package com.fallguys.review.service;

import com.fallguys.review.api.dto.request.EmployerRejectionReasonCreateRequest;
import com.fallguys.review.api.dto.response.EmployerRejectionReasonResponseDTO;
import com.fallguys.review.entity.EmployerRejectionReason;
import com.fallguys.review.repository.EmployerRejectionReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployerRejectionReasonServiceImpl implements EmployerRejectionReasonService {

    private final EmployerRejectionReasonRepository employerRejectionReasonRepository;

    @Override
    @Transactional
    public Long createEmployerRejectionReason(Long employerId, EmployerRejectionReasonCreateRequest request) {
        EmployerRejectionReason reason = EmployerRejectionReason.builder()
                .projectId(request.projectId())
                .projectTitle(request.projectTitle().trim())
                .employerId(employerId)
                .freelancerId(request.freelancerId())
                .reason(request.reason().trim())
                .build();
        return employerRejectionReasonRepository.save(reason).getId();
    }

    @Override
    public Page<EmployerRejectionReasonResponseDTO> getEmployerRejectionReasons(Long employerId, String title, Pageable pageable) {
        String normalizedTitle = normalizeTitle(title);
        Page<EmployerRejectionReason> result = StringUtils.hasText(normalizedTitle)
                ? employerRejectionReasonRepository.findAllByEmployerIdAndProjectTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                employerId,
                normalizedTitle,
                pageable
        )
                : employerRejectionReasonRepository.findAllByEmployerIdOrderByCreatedAtDesc(employerId, pageable);

        return result.map(this::toResponse);
    }

    @Override
    public Page<EmployerRejectionReasonResponseDTO> getFreelancerRejectionReasons(Long freelancerId, String title, Pageable pageable) {
        String normalizedTitle = normalizeTitle(title);
        Page<EmployerRejectionReason> result = StringUtils.hasText(normalizedTitle)
                ? employerRejectionReasonRepository.findAllByFreelancerIdAndProjectTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                freelancerId,
                normalizedTitle,
                pageable
        )
                : employerRejectionReasonRepository.findAllByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable);

        return result.map(this::toResponse);
    }

    private EmployerRejectionReasonResponseDTO toResponse(EmployerRejectionReason rejectionReason) {
        return new EmployerRejectionReasonResponseDTO(
                rejectionReason.getId(),
                rejectionReason.getProjectId(),
                rejectionReason.getProjectTitle(),
                rejectionReason.getEmployerId(),
                rejectionReason.getFreelancerId(),
                rejectionReason.getReason(),
                rejectionReason.getCreatedAt()
        );
    }

    private String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        return title.trim();
    }
}
