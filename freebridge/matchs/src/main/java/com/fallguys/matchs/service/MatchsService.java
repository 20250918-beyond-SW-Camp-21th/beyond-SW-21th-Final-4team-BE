package com.fallguys.matchs.service;

import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;
import com.fallguys.matchs.api.dto.response.ApplicationResponseDTO;
import com.fallguys.matchs.api.dto.response.ProposalResponseDTO;

import java.util.List;

public interface MatchsService {
    Long createApplication(Long freelancerId, ApplicationCreateRequest request);

    Long createProposal(Long employerId, ProposalCreateRequest request);

    Long acceptApplication(Long employerId, Long applicationId);

    Long acceptProposal(Long freelancerId, Long proposalId);

    Long rejectApplication(Long employerId, Long applicationId);

    Long rejectProposal(Long freelancerId, Long proposalId);

    List<ApplicationResponseDTO> getEmployerApplications(Long employerId);

    ApplicationResponseDTO getEmployerApplication(Long employerId, Long applicationId);

    ProposalResponseDTO getEmployerProposal(Long employerId, Long proposalId);

    List<ApplicationResponseDTO> getFreelancerApplications(Long freelancerId);

    ApplicationResponseDTO getFreelancerApplication(Long freelancerId, Long applicationId);

    List<ProposalResponseDTO> getFreelancerProposals(Long freelancerId);

    ProposalResponseDTO getFreelancerProposal(Long freelancerId, Long proposalId);
}
