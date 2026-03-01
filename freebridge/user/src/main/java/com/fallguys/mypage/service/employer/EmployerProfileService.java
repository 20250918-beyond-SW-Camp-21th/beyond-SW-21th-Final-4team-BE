package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployerProfileService {

    private final EmployerRepository employerRepository;

    @Transactional(readOnly = true)
    public EmployerBasicProfileDto getProfile(Long userId) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

        return new EmployerBasicProfileDto(
                employer.getCompanyName(),
                employer.getIndustry(),
                employer.getScale() != null ? employer.getScale().name() : null,
                employer.getLocation(),
                employer.getWebsiteUrl(),
                employer.getDescription(),
                employer.getLogoUrl(),
                employer.getStatus() != null ? employer.getStatus().name() : null
        );
    }
}
