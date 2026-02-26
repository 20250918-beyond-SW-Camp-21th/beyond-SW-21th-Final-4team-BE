package com.fallguys.recruitment.infra.user;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.service.port.RecruitmentUser;
import com.fallguys.recruitment.service.port.RecruitmentUserReader;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentUserReaderImpl implements RecruitmentUserReader {

    private final UserRepository userRepository;

    @Override
    public RecruitmentUser getEmployerByIdOrThrow(Long userId) {
        User user = getByIdOrThrow(userId);
        if (user.getRole() != Role.EMPLOYER) {
            throw new BusinessException(ErrorCode.ONLY_EMPLOYER_ALLOWED);
        }
        return new RecruitmentUser(user.getId(), user.getName());
    }

    @Override
    public RecruitmentUser getFreelancerByIdOrThrow(Long userId) {
        User user = getByIdOrThrow(userId);
        if (user.getRole() != Role.FREELANCER) {
            throw new BusinessException(ErrorCode.ONLY_FREELANCER_ALLOWED);
        }
        return new RecruitmentUser(user.getId(), user.getName());
    }

    private User getByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
