package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.api.web.dto.freelancer.request.GradeSaveRequestDto;
import com.fallguys.mypage.entity.freelancer.AcademicDegree;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.FreelancerGrade;
import com.fallguys.mypage.entity.freelancer.LicenseGrade;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FreelancerGradeService {

    private final FreelancerRepository freelancerRepository;

    @Transactional
    public void saveGrade(Long userId, GradeSaveRequestDto request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String type = request.type().trim().toLowerCase();
        if (!type.equals("education") && !type.equals("certification")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (type.equals("education")) {
            if (request.education() == null || request.education().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            validateAcademicDegree(request.education());
        } else {
            if (request.certification() == null || request.certification().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            validateLicenseGrade(request.certification());
        }

        FreelancerGrade grade = parseFreelancerGrade(request.grade());

        Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        freelancer.changeGrade(grade);
    }

    private void validateAcademicDegree(String value) {
        String normalized = value.trim();
        for (AcademicDegree degree : AcademicDegree.values()) {
            if (degree.name().equalsIgnoreCase(normalized) || degree.getDescription().equals(normalized)) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private void validateLicenseGrade(String value) {
        String normalized = value.trim();
        for (LicenseGrade grade : LicenseGrade.values()) {
            if (grade.name().equalsIgnoreCase(normalized) || grade.getDescription().equals(normalized)) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private FreelancerGrade parseFreelancerGrade(String value) {
        String normalized = value.trim();
        switch (normalized) {
            case "초급":
                return FreelancerGrade.JUNIOR;
            case "중급":
                return FreelancerGrade.INTERMEDIATE;
            case "고급":
                return FreelancerGrade.SENIOR;
            case "특급":
                return FreelancerGrade.MASTER;
            default:
                break;
        }

        try {
            return FreelancerGrade.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
