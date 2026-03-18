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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        RecruitmentUser freelancer = getFreelancersByIdsOrThrow(List.of(userId)).get(userId);
        if (freelancer == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return freelancer;
    }

    @Override
    public RecruitmentUser getFreelancerByFreelancerIdOrThrow(Long freelancerId) {
        RecruitmentUser freelancer = getFreelancersByFreelancerIdsOrThrow(List.of(freelancerId)).get(freelancerId);
        if (freelancer == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return freelancer;
    }

    @Override
    public Map<Long, RecruitmentUser> getFreelancersByIdsOrThrow(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<Long> uniqueUserIds = new LinkedHashSet<>(userIds);
        Map<Long, User> usersById = userRepository.findAllById(uniqueUserIds)
                .stream()
                .collect(LinkedHashMap::new, (map, user) -> map.put(user.getId(), user), Map::putAll);

        for (Long userId : uniqueUserIds) {
            User user = usersById.get(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (user.getRole() != Role.FREELANCER) {
                throw new BusinessException(ErrorCode.ONLY_FREELANCER_ALLOWED);
            }
        }

        Map<Long, com.fallguys.mypage.entity.freelancer.Freelancer> freelancersByUserId =
                freelancerRepository.findAllByUserIdIn(uniqueUserIds)
                        .stream()
                        .collect(LinkedHashMap::new, (map, freelancer) -> map.put(freelancer.getUserId(), freelancer), Map::putAll);

        Map<Long, RecruitmentUser> result = new LinkedHashMap<>();
        for (Long userId : uniqueUserIds) {
            User user = usersById.get(userId);
            var freelancer = freelancersByUserId.get(userId);
            if (freelancer == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            result.put(userId, toRecruitmentFreelancer(user.getId(), user, freelancer));
        }
        return result;
    }

    @Override
    public Map<Long, RecruitmentUser> getFreelancersByFreelancerIdsOrThrow(Collection<Long> freelancerIds) {
        if (freelancerIds == null || freelancerIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<Long> uniqueFreelancerIds = new LinkedHashSet<>(freelancerIds);
        Map<Long, com.fallguys.mypage.entity.freelancer.Freelancer> freelancersById =
                freelancerRepository.findAllById(uniqueFreelancerIds)
                        .stream()
                        .collect(LinkedHashMap::new, (map, freelancer) -> map.put(freelancer.getFreelancerId(), freelancer), Map::putAll);

        for (Long freelancerId : uniqueFreelancerIds) {
            if (!freelancersById.containsKey(freelancerId)) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
        }

        LinkedHashSet<Long> userIds = freelancersById.values().stream()
                .map(com.fallguys.mypage.entity.freelancer.Freelancer::getUserId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Map<Long, User> usersById = userRepository.findAllById(userIds)
                .stream()
                .collect(LinkedHashMap::new, (map, user) -> map.put(user.getId(), user), Map::putAll);

        Map<Long, RecruitmentUser> result = new LinkedHashMap<>();
        for (Long freelancerId : uniqueFreelancerIds) {
            var freelancer = freelancersById.get(freelancerId);
            User user = usersById.get(freelancer.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (user.getRole() != Role.FREELANCER) {
                throw new BusinessException(ErrorCode.ONLY_FREELANCER_ALLOWED);
            }
            result.put(freelancerId, toRecruitmentFreelancer(freelancerId, user, freelancer));
        }
        return result;
    }

    private RecruitmentUser toRecruitmentFreelancer(Long exposedId, User user, com.fallguys.mypage.entity.freelancer.Freelancer freelancer) {
        return new RecruitmentUser(
                exposedId,
                user.getName(),
                Optional.ofNullable(freelancer.getSkills())
                        .orElse(List.of())
                        .stream()
                        .filter(skill -> skill != null && !skill.isBlank())
                        .map(String::trim)
                        .collect(java.util.stream.Collectors.joining(",")),
                Optional.ofNullable(freelancer.getIntroduction()).orElse("정보 없음"),
                Optional.ofNullable(freelancer.getStatus()).map(Enum::name).orElse("POTENTIAL")
        );
    }

    private User getByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
