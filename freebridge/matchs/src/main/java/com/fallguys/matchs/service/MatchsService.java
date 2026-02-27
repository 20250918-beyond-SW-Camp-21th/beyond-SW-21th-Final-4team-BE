package com.fallguys.matchs.service;

import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;
import com.fallguys.matchs.api.dto.response.ApplicationResponseDTO;
import com.fallguys.matchs.api.dto.response.ProposalResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchsService {
    Long createApplication(Long freelancerId, ApplicationCreateRequest request);

    Long createProposal(Long employerId, ProposalCreateRequest request);

    Long acceptApplication(Long employerId, Long applicationId);

    Long acceptProposal(Long freelancerId, Long proposalId);

    Long rejectApplication(Long employerId, Long applicationId);

    Long rejectProposal(Long freelancerId, Long proposalId);

    Page<ApplicationResponseDTO> getEmployerApplications(Long employerId, Pageable pageable);

    ApplicationResponseDTO getEmployerApplication(Long employerId, Long applicationId);

    Page<ProposalResponseDTO> getEmployerProposals(Long employerId, Pageable pageable);

    ProposalResponseDTO getEmployerProposal(Long employerId, Long proposalId);

    Page<ApplicationResponseDTO> getFreelancerApplications(Long freelancerId, Pageable pageable);

    ApplicationResponseDTO getFreelancerApplication(Long freelancerId, Long applicationId);

    Page<ProposalResponseDTO> getFreelancerProposals(Long freelancerId, Pageable pageable);

    ProposalResponseDTO getFreelancerProposal(Long freelancerId, Long proposalId);
}
