package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.time.LocalDateTime;

public record PortfolioInfoDto(
        String fileUrl,
        String fileName,
        LocalDateTime lastUpdated
) {}
