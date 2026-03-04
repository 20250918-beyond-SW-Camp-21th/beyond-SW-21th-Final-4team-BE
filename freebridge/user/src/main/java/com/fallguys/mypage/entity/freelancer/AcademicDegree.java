package com.fallguys.mypage.entity.freelancer;

/**
 * 학경력자 등급 산정 시 사용하는 최종학력 Enum
 * - ASSOCIATE: 전문학사 (2년제)
 * - BACHELOR : 학사    (4년제)
 * - MASTER   : 석사
 * - DOCTOR   : 박사
 */
public enum AcademicDegree {
    ASSOCIATE("전문학사"),
    BACHELOR("학사"),
    MASTER("석사"),
    DOCTOR("박사");

    private final String description;

    AcademicDegree(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
