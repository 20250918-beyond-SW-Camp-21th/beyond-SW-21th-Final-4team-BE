package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;

public interface SubscriptionBillingService {

    PageResponse<SubscriptionBillingItem> getBillingHistory(Long employerId, String status, int page, int size);
}
