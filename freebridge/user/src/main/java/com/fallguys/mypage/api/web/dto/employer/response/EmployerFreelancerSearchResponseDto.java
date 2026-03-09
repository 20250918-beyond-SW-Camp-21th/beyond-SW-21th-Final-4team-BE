package com.fallguys.mypage.api.web.dto.employer.response;

import java.util.List;

public record EmployerFreelancerSearchResponseDto(
        List<EmployerFreelancerSearchItemDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
