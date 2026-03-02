package com.fallguys.payment.service.impl;

import com.fallguys.payment.entity.BillingKey;
import com.fallguys.payment.repository.BillingKeyRepository;
import com.fallguys.payment.service.BillingKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingKeyServiceImpl implements BillingKeyService {

    private final BillingKeyRepository billingKeyRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<BillingKey> getActiveBillingKey(Long employerId) {
        return billingKeyRepository.findByEmployerIdAndActiveTrue(employerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingKey> getAllActiveBillingKeys() {
        return billingKeyRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public void deactivateBillingKey(Long employerId) {
        billingKeyRepository.findByEmployerIdAndActiveTrue(employerId)
                .ifPresent(bk -> {
                    bk.deactivate();
                    billingKeyRepository.save(bk);
                });
    }
}
