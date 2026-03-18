package com.fallguys.user.service;

import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.user.api.shared.ExternalFreelancerSearchApi;
import com.fallguys.user.api.shared.response.ExternalFreelancerSearchItem;
import com.fallguys.user.api.shared.response.ExternalFreelancerSearchResponse;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalFreelancerSearchApiImpl implements ExternalFreelancerSearchApi {

    private final FreelancerRepository freelancerRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    @Override
    @Transactional(readOnly = true)
    public ExternalFreelancerSearchResponse searchFreelancers(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Freelancer> result = freelancerRepository.searchFreelancers(keyword, pageable);

        Map<Long, String> userNameMap = loadUserNames(result.getContent());

        List<ExternalFreelancerSearchItem> items = result.getContent().stream()
                .map(freelancer -> toItem(freelancer, userNameMap))
                .toList();

        return new ExternalFreelancerSearchResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private Map<Long, String> loadUserNames(List<Freelancer> freelancers) {
        if (freelancers == null || freelancers.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> userIds = freelancers.stream()
                .map(Freelancer::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> map = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            map.put(user.getId(), user.getName());
        }
        return map;
    }

    private ExternalFreelancerSearchItem toItem(Freelancer freelancer, Map<Long, String> userNameMap) {
        String name = userNameMap.getOrDefault(freelancer.getUserId(), "");
        return new ExternalFreelancerSearchItem(
                freelancer.getFreelancerId(),
                freelancer.getUserId(),
                name,
                freelancer.getJob(),
                freelancer.getCareerYears(),
                freelancer.getWage(),
                freelancer.getIntroduction(),
                toAccessibleUrl(freelancer.getAvatarUrl()),
                freelancer.getSkills() == null ? List.of() : freelancer.getSkills(),
                freelancer.getGrade() == null ? null : freelancer.getGrade().name()
        );
    }

    private String toAccessibleUrl(String storedKeyOrUrl) {
        if (storedKeyOrUrl == null || storedKeyOrUrl.isBlank()) {
            return null;
        }
        if (storedKeyOrUrl.startsWith("http://") || storedKeyOrUrl.startsWith("https://")) {
            return storedKeyOrUrl;
        }
        try {
            return fileStorage.generatePresignedUrl(storedKeyOrUrl);
        } catch (RuntimeException e) {
            log.error("Failed to generate freelancer avatar URL. key={}", storedKeyOrUrl, e);
            return null;
        }
    }
}
