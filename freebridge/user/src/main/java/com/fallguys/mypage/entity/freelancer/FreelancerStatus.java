package com.fallguys.mypage.entity.freelancer;

public enum FreelancerStatus {
    POTENTIAL("잠재"),
    CONTRACT_REVIEW("계약심사"),
    CONTRACT_IN_PROGRESS("계약진행"),
    CONTRACTING("계약중"),
    CONTRACT_EXPIRED("계약만료"),
    LEFT("이탈");

    private final String description;

    FreelancerStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
