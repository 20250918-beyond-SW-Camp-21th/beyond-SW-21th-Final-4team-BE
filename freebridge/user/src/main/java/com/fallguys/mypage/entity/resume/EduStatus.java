package com.fallguys.mypage.entity.resume;

public enum EduStatus {
    LEAVE_OF_ABSENCE("휴학"),
    ATTENDING("재학"),
    GRADUATED("졸업"),
    OTHER("기타");

    private final String description;

    EduStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
