package com.fallguys.matchs.entity;

import java.time.LocalDateTime;
import java.util.UUID;

//프리랜서->기업
public class Application {

    private String id = UUID.randomUUID().toString();

    private String jobPosting;

    private String freelancerId;

    private String employerId;

    private String message;

    private MatchsStatus status=MatchsStatus.PENDING;

    private LocalDateTime createdAt=LocalDateTime.now();

}
