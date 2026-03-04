package com.fallguys.mypage.api.web.dto.freelancer.request;

import com.fallguys.mypage.entity.freelancer.AcademicDegree;
import com.fallguys.mypage.entity.freelancer.LicenseGrade;

/**
 * 프리랜서 등급 산정 요청 DTO
 *
 * 1. 학경력자(ACADEMIC_CAREER): qualificationType + degree + careerYears
 * 2. 자격자  (LICENSED)        : qualificationType + licenseGrade + careerYears
 */
public record GradeCalculationRequestDto(
        QualificationType qualificationType, // 산정 방식 (필수)

        // ─── 학경력자 필드 ───────────────────────────────────────
        AcademicDegree degree,              // 최종학력 (학경력자만 필수)

        // ─── 자격자 필드 ─────────────────────────────────────────
        LicenseGrade licenseGrade,          // 자격증 구분 (자격자만 필수)

        // ─── 공통 필드 ───────────────────────────────────────────
        Integer careerYears                 // 경력 연수 (0 이상)
) {}
