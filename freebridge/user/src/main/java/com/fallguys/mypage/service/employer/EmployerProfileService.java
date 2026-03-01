package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto;
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

        return EmployerBasicProfileDto.from(employer);
    }

    @Transactional
    public void updateProfile(Long userId, EmployerProfileUpdateRequestDto request) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

        employer.updateProfile(
                request.companyName(),
                request.industry(),
                request.scale() != null ? com.fallguys.mypage.entity.employer.Scale.valueOf(request.scale()) : null,
                request.location(),
                request.websiteUrl(),
                request.description(),
                employer.getLogoUrl() // 기존 로고는 유지 (로고 수정 API 분리됨)
        );
    }
}
