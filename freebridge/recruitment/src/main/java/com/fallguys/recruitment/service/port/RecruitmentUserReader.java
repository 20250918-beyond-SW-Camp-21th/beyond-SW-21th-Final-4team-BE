package com.fallguys.recruitment.service.port;

public interface RecruitmentUserReader {

    RecruitmentUser getEmployerByIdOrThrow(Long userId);

    RecruitmentUser getFreelancerByIdOrThrow(Long userId);
}
