package com.fallguys.matchs.entity;

import java.time.LocalDateTime;
//기업->프리랜서
public class Proposal {

    private String id;

    private String jobPosting;

    private String freelancerId;

    private String employerId;

    private String message;

    private MatchsStatus status=MatchsStatus.PENDING;

    private LocalDateTime createdAt=LocalDateTime.now();
}
