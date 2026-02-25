package com.fallguys.matchs.service;

import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;

public interface MatchsService {
    Long createApplication(Long freelancerId, ApplicationCreateRequest request);

    Long createProposal(Long employerId, ProposalCreateRequest request);

    Long acceptApplication(Long employerId, Long applicationId);

    Long acceptProposal(Long freelancerId, Long proposalId);
}
