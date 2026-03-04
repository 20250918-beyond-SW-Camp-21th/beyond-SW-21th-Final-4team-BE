package com.fallguys.mypage.api.web.dto.resume.request;

import java.time.LocalDate;

/**
 * 이력서 기본 정보(인적사항) 수정 요청 DTO
 * 학력/경력/자격증은 별도 엔드포인트로 관리됩니다.
 */
public record ResumeBasicInfoRequestDto(
        String name,
        LocalDate birthDate,
        String phone,
        String email,
        String address
) {}
