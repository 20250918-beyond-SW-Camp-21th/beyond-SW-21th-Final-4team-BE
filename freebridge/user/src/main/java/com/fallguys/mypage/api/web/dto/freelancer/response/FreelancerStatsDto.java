package com.fallguys.mypage.api.web.dto.freelancer.response;

public record FreelancerStatsDto(
        Integer statContact, // 노출/조회수
        Integer statChat, // 제안받은 횟수
        Integer statContract // 누적 계약 건수
) {}
