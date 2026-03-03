package com.fallguys.mypage.api.web.dto.freelancer.request;

/**
 * 등급 산정 방식 선택 Enum
 * - ACADEMIC_CAREER : 학경력자 (학력 + 경력 연수 기반)
 * - LICENSED        : 자격자   (자격증 등급 + 경력 연수 기반)
 */
public enum QualificationType {
    ACADEMIC_CAREER, // 학경력자
    LICENSED         // 자격자
}
