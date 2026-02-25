package com.fallguys.mypage.entity.employer;

public enum EmployerStatus {
    POTENTIAL("잠재"),
    ACTIVE("활성"),
    CONTRACT_REVIEW("계약심사"),
    CONTRACT_IN_PROGRESS("계약진행"),
    CONTRACTING("계약중"),
    CONTRACT_EXPIRED("계약만료"),
    LEFT("이탈");

    private final String description;

    EmployerStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
