package com.fallguys.review.service;

import com.fallguys.review.api.shared.ExternalFreelancerReviewApi;
import com.fallguys.review.api.shared.response.FreelancerReviewMetricsDto;
import com.fallguys.review.api.shared.response.FreelancerReviewMetricsProjection;
import com.fallguys.review.repository.EmployerReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ExternalFreelancerReviewApiImpl implements ExternalFreelancerReviewApi {

    private final EmployerReviewRepository employerReviewRepository;

    @Override
    @Transactional(readOnly = true)
    public FreelancerReviewMetricsDto getFreelancerReviewMetrics(Long userId) {
        if (userId == null) {
            return FreelancerReviewMetricsDto.empty();
        }

        FreelancerReviewMetricsProjection projection =
                employerReviewRepository.findFreelancerReviewMetrics(userId);

        if (projection == null) {
            return FreelancerReviewMetricsDto.empty();
        }

        double programming = round1(numberValue(projection.getProgramming()));
        double framework = round1(numberValue(projection.getFramework()));
        double debugging = round1(numberValue(projection.getDebugging()));
        double communication = round1(numberValue(projection.getCommunication()));
        double schedule = round1(numberValue(projection.getSchedule()));
        double dispute = round1(numberValue(projection.getDispute()));

        if (programming == 0.0
                && framework == 0.0
                && debugging == 0.0
                && communication == 0.0
                && schedule == 0.0
                && dispute == 0.0) {
            return FreelancerReviewMetricsDto.empty();
        }

        double averageRate = round1((programming + framework + debugging + communication + schedule + dispute) / 6.0);

        return new FreelancerReviewMetricsDto(
                programming,
                framework,
                debugging,
                communication,
                schedule,
                dispute,
                averageRate
        );
    }

    private double numberValue(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
