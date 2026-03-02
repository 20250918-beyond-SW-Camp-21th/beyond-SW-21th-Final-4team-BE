package com.fallguys.payment.service;

import com.fallguys.payment.entity.BillingKey;

import java.util.List;
import java.util.Optional;

public interface BillingKeyService {

    Optional<BillingKey> getActiveBillingKey(Long employerId);

    List<BillingKey> getAllActiveBillingKeys();

    void deactivateBillingKey(Long employerId);
}
