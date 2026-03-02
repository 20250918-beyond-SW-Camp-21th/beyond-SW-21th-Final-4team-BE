package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.SubscriptionBillingStatus;
import com.fallguys.payment.repository.SubscriptionBillingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingService {

    private final SubscriptionBillingRepository subscriptionBillingRepository;

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionBillingItem> getBillingHistory(Long employerId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size,
                Sort.by(Sort.Direction.DESC, "billingDate"));

        var pageResult = "ALL".equalsIgnoreCase(status)
                ? subscriptionBillingRepository.findByEmployerId(employerId, pageable)
                : subscriptionBillingRepository.findByEmployerIdAndStatus(
                        employerId, SubscriptionBillingStatus.valueOf(status), pageable);

        List<SubscriptionBillingItem> items = pageResult.getContent().stream()
                .map(b -> new SubscriptionBillingItem(
                        b.getId(), b.getPlanType().name(), b.getAmount(),
                        b.getStatus().name(), b.getBillingDate(), b.getPaidDate()))
                .toList();

        return new PageResponse<>(items, pageResult.getTotalElements(),
                pageResult.getTotalPages(), page);
    }
}