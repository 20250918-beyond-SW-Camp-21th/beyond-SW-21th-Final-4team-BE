package com.fallguys.recruitment.service.port;

import java.util.Collection;
import java.util.Map;

public interface RecruitmentUserReader {

    RecruitmentUser getEmployerByIdOrThrow(Long userId);

    RecruitmentUser getFreelancerByIdOrThrow(Long userId);

    Map<Long, RecruitmentUser> getFreelancersByIdsOrThrow(Collection<Long> userIds);
}
