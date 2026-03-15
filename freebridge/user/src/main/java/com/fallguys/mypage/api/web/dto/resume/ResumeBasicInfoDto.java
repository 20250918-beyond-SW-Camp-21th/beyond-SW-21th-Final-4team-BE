package com.fallguys.mypage.api.web.dto.resume;

import java.time.LocalDate;

public record ResumeBasicInfoDto(
        String name,
        LocalDate birthDate,
        String phone,
        String email,
        String address
) {}
