package com.fallguys.mypage.entity.freelancer;

/**
 * 자격자 등급 산정 시 사용하는 자격증 구분 Enum
 * - ENGINEER_PROFESSIONAL: 기술사 (기사 취득 후 일정 경력)
 * - ENGINEER             : 기사   (정보처리기사 등)
 * - INDUSTRIAL_ENGINEER  : 산업기사
 * - TECHNICIAN           : 기능사
 */
public enum LicenseGrade {
    ENGINEER_PROFESSIONAL("기술사"),
    ENGINEER("기사"),
    INDUSTRIAL_ENGINEER("산업기사"),
    TECHNICIAN("기능사");

    private final String description;

    LicenseGrade(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
