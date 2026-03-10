package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.time.LocalDate;

public record WorkConditionsDto(
        String workType,
        LocalDate availableStartDate,
        String workStyle,
        String workLocation
) {}
