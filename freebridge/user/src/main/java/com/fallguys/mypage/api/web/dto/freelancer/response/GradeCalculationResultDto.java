package com.fallguys.mypage.api.web.dto.freelancer.response;

import com.fallguys.mypage.entity.freelancer.FreelancerGrade;

/**
 * 등급 산정 결과 응답 DTO
 */
public record GradeCalculationResultDto(
        FreelancerGrade grade,        // 산정된 등급 (JUNIOR/INTERMEDIATE/SENIOR/MASTER)
        String gradeDescription,      // 등급 한글명 (초급/중급/고급/특급)
        String basis,                 // 산정 근거 설명
        Integer careerYears,          // 입력된 경력 연수
        String qualificationType      // 산정 방식 (학경력자/자격자)
) {
    public static GradeCalculationResultDto of(FreelancerGrade grade, String basis,
                                               Integer careerYears, String qualificationType) {
        return new GradeCalculationResultDto(grade, gradeLabel(grade), basis, careerYears, qualificationType);
    }

    private static String gradeLabel(FreelancerGrade grade) {
        return switch (grade) {
            case JUNIOR       -> "초급";
            case INTERMEDIATE -> "중급";
            case SENIOR       -> "고급";
            case MASTER       -> "특급";
        };
    }
}
