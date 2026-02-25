package com.fallguys.recruitment.service.port;

public interface RecruitmentUserReader {

    RecruitmentUser getEmployerByEmailOrThrow(String email);

    RecruitmentUser getFreelancerByEmailOrThrow(String email);
}
