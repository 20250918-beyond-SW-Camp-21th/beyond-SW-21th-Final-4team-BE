package com.fallguys.user.api.shared.response;

import java.util.List;

public record ExternalFreelancerSearchResponse(
        List<ExternalFreelancerSearchItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
