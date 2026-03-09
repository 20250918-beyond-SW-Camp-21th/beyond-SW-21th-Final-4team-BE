package com.fallguys.recruitment.infra.user;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.recruitment.service.port.RecruitmentUser;
import com.fallguys.recruitment.service.port.RecruitmentUserReader;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecruitmentUserReaderImpl implements RecruitmentUserReader {

    private final UserRepository userRepository;
    private final FreelancerRepository freelancerRepository;

    @Override
    public RecruitmentUser getEmployerByIdOrThrow(Long userId) {
        User user = getByIdOrThrow(userId);
        if (user.getRole() != Role.EMPLOYER) {
            throw new BusinessException(ErrorCode.ONLY_EMPLOYER_ALLOWED);
        }
        return new RecruitmentUser(user.getId(), user.getName(), null, null, null);
    }

    @Override
    public RecruitmentUser getFreelancerByIdOrThrow(Long userId) {
        User user = getByIdOrThrow(userId);

        if (user.getRole() != Role.FREELANCER) {
            throw new BusinessException(ErrorCode.ONLY_FREELANCER_ALLOWED);
        }

        var freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new RecruitmentUser(
                user.getId(),
                user.getName(),
                Optional.ofNullable(freelancer.getSkills()).map(Object::toString).orElse("[]"),
                Optional.ofNullable(freelancer.getIntroduction()).orElse("정보 없음"),
                Optional.ofNullable(freelancer.getStatus()).map(Enum::name).orElse("POTENTIAL")
        );
    }

    private User getByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}