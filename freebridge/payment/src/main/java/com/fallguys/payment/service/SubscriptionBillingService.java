package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
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
        public PageResponse<SubscriptionBillingItem> getBillingHistory(Long employerId, String status, int page,
                        int size) {
                if (size <= 0) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                }
                Pageable pageable = PageRequest.of(Math.max(0, page - 1), size,
                                Sort.by(Sort.Direction.DESC, "billingDate"));

                SubscriptionBillingStatus parsedStatus = null;
                if (!"ALL".equalsIgnoreCase(status)) {
                        try {
                                String statusUpper = status != null ? status.trim().toUpperCase() : "";
                                parsedStatus = SubscriptionBillingStatus.valueOf(statusUpper);
                        } catch (IllegalArgumentException e) {
                                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                        }
                }

                var pageResult = (parsedStatus == null)
                                ? subscriptionBillingRepository.findByEmployerId(employerId, pageable)
                                : subscriptionBillingRepository.findByEmployerIdAndStatus(employerId, parsedStatus,
                                                pageable);

                List<SubscriptionBillingItem> items = pageResult.getContent().stream()
                                .map(b -> new SubscriptionBillingItem(
                                                b.getId(), b.getPlanType().name(), b.getAmount(),
                                                b.getStatus().name(), b.getBillingDate(), b.getPaidDate(),
                                                b.getInvoicePdfUrl()))
                                .toList();

                return new PageResponse<>(items, pageResult.getTotalElements(),
                                pageResult.getTotalPages(), page);
        }

        @Transactional(readOnly = true)
        public String getInvoicePdfUrl(Long employerId, Long billingId) {
                var billing = subscriptionBillingRepository.findById(billingId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

                if (!billing.getEmployerId().equals(employerId)) {
                        throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
                }

                return billing.getInvoicePdfUrl();
        }
}
