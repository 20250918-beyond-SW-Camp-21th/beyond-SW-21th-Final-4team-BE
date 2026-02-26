package com.fallguys.matchs.api.dto.request;

public record ProposalCreateRequest(
        Long jobPostingId,
        Long freelancerId,
        String message
) {
}
